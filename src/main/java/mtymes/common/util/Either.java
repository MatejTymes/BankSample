package mtymes.common.util;

import java.util.NoSuchElementException;

// todo: test this
public abstract class Either<L, R> {

    private Either() {
    }

    public static <L, R> Either<L, R> right(R value) {
        return new Either.Right<L, R>(value);
    }

    public static <L, R> Either<L, R> left(L value) {
        return new Either.Left<L, R>(value);
    }

    public boolean isLeft() {
        return false;
    }

    public boolean isRight() {
        return false;
    }

    public R getRight() {
        throw new NoSuchElementException("Left value not present");
    }

    public L getLeft() {
        throw new NoSuchElementException("Left value not present");
    }

    public Object handleAndGet(Runnable onRightTask, Runnable onLeftTask) {
        if (isRight()) {
            onRightTask.run();
            return getRight();
        } else {
            onLeftTask.run();
            return getLeft();
        }
    }

    public abstract Object get();

    public static final class Right<L, R> extends Either<L, R> {

        private final R value;

        private Right(R value) {
            this.value = value;
        }

        @Override
        public boolean isRight() {
            return true;
        }

        @Override
        public R getRight() {
            return value;
        }

        @Override
        public Object get() {
            return value;
        }
    }

    public static final class Left<L, R> extends Either<L, R> {

        private final L value;

        private Left(L value) {
            this.value = value;
        }

        @Override
        public boolean isLeft() {
            return true;
        }

        @Override
        public L getLeft() {
            return value;
        }

        @Override
        public Object get() {
            return value;
        }
    }
}
