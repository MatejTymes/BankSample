package mtymes.test;

import javafixes.object.DataObject;

import java.util.List;

import static java.util.Collections.unmodifiableList;
import static javafixes.common.CollectionUtil.newList;

class MethodCall extends DataObject {

    final String methodName;
    final List<Object> params;

    public MethodCall(String methodName, Object... params) {
        this.methodName = methodName;
        this.params = unmodifiableList(newList(params));
    }
}
