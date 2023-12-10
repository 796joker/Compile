package error;

import java.util.ArrayList;
import java.util.List;

public class ErrorHandler {
    private static final ErrorHandler instance = new ErrorHandler();
    public static ErrorHandler getErrorHandler() {
        return instance;
    }
    private List<DefError> errorList;
    public void init() {
        this.errorList = new ArrayList<DefError>();
    }
    private ErrorHandler() {
    }

    /**
     * 记录产生的错误
     */
    public void addError(int lineNum, ErrorType type) {
        DefError error = new DefError(lineNum, type);
        this.errorList.add(error);
    }

    public List<DefError> getErrorList() {
        return errorList;
    }

    public boolean ifErrorExist() {
        return this.errorList.size() == 0;
    }
}
