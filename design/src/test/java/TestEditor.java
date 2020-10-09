import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import kotlin.NotImplementedError;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

public class TestEditor implements Editor {
    @Override
    public Document getDocument() {
        throw new RuntimeException();
        //return null;
    }

    @Override
    public boolean isViewer() {
        return false;
    }

    @Override
    public JComponent getComponent() {
        return null;
    }

    @Override
    public JComponent getContentComponent() {
        return null;
    }

    @Override
    public void setBorder(Border border) {

    }

    @Override
    public Insets getInsets() {
        return null;
    }

    @Override
    public SelectionModel getSelectionModel() {
        return null;
    }

    @Override
    public MarkupModel getMarkupModel() {
        return null;
    }

    @Override
    public FoldingModel getFoldingModel() {
        return null;
    }

    @Override
    public ScrollingModel getScrollingModel() {
        return null;
    }

    @Override
    public CaretModel getCaretModel() {
        return null;
    }

    @Override
    public SoftWrapModel getSoftWrapModel() {
        return null;
    }

    @Override
    public EditorSettings getSettings() {
        return null;
    }

    @Override
    public EditorColorsScheme getColorsScheme() {
        return null;
    }

    @Override
    public int getLineHeight() {
        return 0;
    }

    @Override
    public Point logicalPositionToXY(LogicalPosition logicalPosition) {
        return null;
    }

    @Override
    public int logicalPositionToOffset(LogicalPosition logicalPosition) {
        return 0;
    }

    @Override
    public VisualPosition logicalToVisualPosition(LogicalPosition logicalPosition) {
        return null;
    }

    @Override
    public Point visualPositionToXY(VisualPosition visualPosition) {
        return null;
    }

    @Override
    public Point2D visualPositionToPoint2D(VisualPosition visualPosition) {
        return null;
    }

    @Override
    public LogicalPosition visualToLogicalPosition(VisualPosition visualPosition) {
        return null;
    }

    @Override
    public LogicalPosition offsetToLogicalPosition(int i) {
        return null;
    }

    @Override
    public VisualPosition offsetToVisualPosition(int i) {
        return null;
    }

    @Override
    public VisualPosition offsetToVisualPosition(int i, boolean b, boolean b1) {
        return null;
    }

    @Override
    public LogicalPosition xyToLogicalPosition(Point point) {
        return null;
    }

    @Override
    public VisualPosition xyToVisualPosition(Point point) {
        return null;
    }

    @Override
    public VisualPosition xyToVisualPosition(Point2D point2D) {
        return null;
    }

    @Override
    public void addEditorMouseListener(EditorMouseListener editorMouseListener) {

    }

    @Override
    public void removeEditorMouseListener(EditorMouseListener editorMouseListener) {

    }

    @Override
    public void addEditorMouseMotionListener(EditorMouseMotionListener editorMouseMotionListener) {

    }

    @Override
    public void removeEditorMouseMotionListener(EditorMouseMotionListener editorMouseMotionListener) {

    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @Override
    public Project getProject() {
        return null;
    }

    @Override
    public boolean isInsertMode() {
        return false;
    }

    @Override
    public boolean isColumnMode() {
        return false;
    }

    @Override
    public boolean isOneLineMode() {
        return false;
    }

    @Override
    public EditorGutter getGutter() {
        return null;
    }

    @Override
    public EditorMouseEventArea getMouseEventArea(MouseEvent mouseEvent) {
        return null;
    }

    @Override
    public void setHeaderComponent(JComponent jComponent) {

    }

    @Override
    public boolean hasHeaderComponent() {
        return false;
    }

    @Override
    public JComponent getHeaderComponent() {
        return null;
    }

    @Override
    public IndentsModel getIndentsModel() {
        return null;
    }

    @Override
    public InlayModel getInlayModel() {
        return null;
    }

    @Override
    public EditorKind getEditorKind() {
        return null;
    }

    @Override
    public <T> T getUserData(Key<T> key) {
        return null;
    }

    @Override
    public <T> void putUserData(Key<T> key, T t) {

    }
}
