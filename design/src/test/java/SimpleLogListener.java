//import java.util.ArrayList;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import org.eclipse.core.runtime.ILogListener;
//import org.eclipse.core.runtime.IStatus;
//
//import com.google.common.base.Throwables;
//
//public class SimpleLogListener implements ILogListener {
//
//    private List<IStatus> messages;
//
//    public SimpleLogListener() {
//        messages = new ArrayList<>();
//    }
//
//    @Override
//    public void logging(IStatus status, String plugin) {
//        messages.add(status);
//    }
//
//    public List<IStatus> getStatuses() {
//        return messages;
//    }
//
//    public List<String> getErrors() {
//        return getMessages(IStatus.ERROR);
//    }
//
//    public List<String> getInfos() {
//        return getMessages(IStatus.INFO);
//    }
//
//    public List<String> getWarnings() {
//        return getMessages(IStatus.WARNING);
//    }
//
//    public List<String> getMessages(int severity) {
//        return getStatuses().stream().filter(s -> s.getSeverity() == severity).map(this::convert).collect(Collectors.toList());
//    }
//
//    private String convert(IStatus status) {
//        StringBuilder s = new StringBuilder(status.getMessage());
//        if (status.getException() != null) {
//            String stackTrace = Throwables.getStackTraceAsString(status.getException());
//            s.append("\n");
//            s.append(stackTrace);
//        }
//        return s.toString();
//    }
//}
