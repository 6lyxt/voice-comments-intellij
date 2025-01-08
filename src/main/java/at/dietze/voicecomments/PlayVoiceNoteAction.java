package at.dietze.voicecomments;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;

public class PlayVoiceNoteAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            Messages.showErrorDialog("No project found.", "Error");
            return;
        }

        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            Messages.showErrorDialog("No editor is currently open.", "Error");
            return;
        }

        String filePath = extractAudioFilePath(editor);
        if (filePath == null) {
            Messages.showErrorDialog("No valid voice note path found on the current line.", "Error");
            return;
        }

        playVoiceNote(filePath, project);
    }

    private String extractAudioFilePath(Editor editor) {
        int caretOffset = editor.getCaretModel().getOffset();

        int lineNumber = editor.getDocument().getLineNumber(caretOffset);

        String lineText = editor.getDocument().getText(new TextRange(
                editor.getDocument().getLineStartOffset(lineNumber),
                editor.getDocument().getLineEndOffset(lineNumber)
        ));

        if (lineText == null || lineText.trim().isEmpty()) {
            return null;
        }

        String filePath = lineText.split(":")[1].trim().replaceAll("[\\[\\]]", "").trim();
        return filePath;
    }

    private String getProjectBasePath(Project project) {
        if (project != null) {
            return project.getBasePath();
        }
        return null;
    }


    private void playVoiceNote(String filePath, Project project) {
        try {
            String projectPath = getProjectBasePath(project);
            if (projectPath == null) {
                Messages.showErrorDialog("Project base path is not available.", "Error");
                return;
            }

            File audioFile = new File(projectPath, filePath);

            if (!audioFile.exists()) {
                Messages.showErrorDialog("The specified voice note file does not exist.", "Error");
                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();

            Messages.showInfoMessage("Playing voice note: " + audioFile.getAbsolutePath(), "Playing");
        } catch (Exception ex) {
            Messages.showErrorDialog("Failed to play the voice note: " + ex.getMessage(), "Error");
        }
    }
}
