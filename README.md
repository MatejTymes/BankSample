# Bank sample

### technical decisions:
- MongoDB is used (as it allowed fast prototyping), (but) this provides few advantages:
  - schema changes can be applied without system downtime or pauses (unlike in most sql solutions) (updates must be designed properly though)
  - secondary nodes can be used which helps with recovery if primary node goes down
- system is designed without the need (and usage) of transactions (how this is accomplished will be described in following sections)
  - the goal was to allow future switching of database if more performant will arise
  - without using transactions the requirement on database types is lowered and different one (or mix of them) could be used (not only sql or non-sql types)

### design decisions:
- each operation (CreateAccount, Transfer, Deposit, Withdraw) is being logged with a sequence id before it is being executed
  - this way we can recreate the db state by just reruning of the log
- each operation is applied to one Account only and each Account has its own operations log (logically separated)
  - so the Transfer between accounts was split into TransferFrom which if successful will submit TransferTo operation to the log
  - this allows to scale system with increased use:
    - if current database allows to process n operations per second, then by storing new accounts (and their operations log) into second/third/... database we'll be able to handle 2x/3x/... as many operations (if the db will prove to be the bottleneck)
  - if there would be some unexpected issues with some account they would be isolated to that account only and should not affect other accounts
- i got rid of transactions applying these principles:
  - each account contains info which Operation has been applied last (account's version field = last applied operation sequence id)
  - only when a previous operation reached its final state (Applied or Rejected) can be the next executed
  - any operation step can fail and be replayed (there are tests to prove this)
    - this makes sure that even if system fails in progress we can self-heal from it without any intervention
  - using CAS (Copy And Swap) principle we make sure that if an operation would be picked and executed by two nodes at the same time the end result would be the same as if it would be executed by single thread (there are tests to prove this)
  - the order of Operation steps is important, but can be easily thought using these few rules:
    - setting Operation.finalState to "Applied" or "Rejected" MUST be the LAST STEP
    - once Operation.finalState is set it can never be changed
    - each modified object other than Operation (like Account) must contain version field which reflects last applied Operation's sequence id (OpLogId.seqId) 
    - if ModifiedObject.version is < Operation.sequenceId, it means the operation step can be applied to the Object (attempt to apply it)
    - if ModifiedObject.version is = Operation.sequenceId, it means operation was applied (do not apply it but continue with next steps - like marking final state)
    - if ModifiedObject.version is > Operation.sequenceId, it means that next operation was already applied (stop handling of this Operation and go to next one (if present))
    - when updating ModifiedObject besides changing its values make sure to change its version to operation's sequenceId but only succeed if the version in database matches your loaded version
    - when Operation submits another Operation, make sure that duplicates will be ignored (for example using unique index on Operation.type and TransferId)

using these principles the Transfer operation can be implemented like this (ignoring checks for Account existence):
- TransferFrom handling
  - if canApplyOperationTo(fromAccount) // fromAccount.version < operation.seqId
    - if insufficientFunds() // fromAccount.balance < operation.amount
      - mark operation as Rejected
    - else
      - updateBalance { fromVersion: fromAccount.version, toVersion: operation.seqId, newBalance: fromAccount.balance - operation.amount }
      - store TransferTo operation
      - mark operation as Applied
  - else if isOperationCurrentlyAppliedTo(fromAccount) // fromAccount.version = operation.seqId
    - store TransferTo operation
    - mark operation as Applied
  - else do nothing
- TransferTo handling
  - if canApplyOperationTo(toAccount) // toAccount.version < operation.seqId
    - updateBalance { fromVersion: toAccount.version, toVersion: operation.seqId, newBalance: toAccount.balance + operation.amount }
    - mark operation as Applied
  - else if isOperationCurrentlyAppliedTo(toAccount) // toAccount.version = operation.seqId
    - mark operation as Applied
  - else do nothing


nice things to add (if i had more time):
- it would be nice to have task to find if there are any "forgotten operation" (could happen in case all nodes would go down) and resubmit them for execution
- currently the Operation Log is stored into MongoDB using "Optimistic Loop". This guarantees that the Operation sequence ids are unique and in storage order, but is slow (stores only few thousand Operations per second). A different/faster database solution might be used for this. 