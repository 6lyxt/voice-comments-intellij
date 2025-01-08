package at.dietze.voicecomments;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class AddVoiceNoteAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
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

        int result = Messages.showYesNoDialog("Would you like to record a voice note?", "Add Voice Note", Messages.getQuestionIcon());

        if (result == Messages.YES) {
            try {
                File voiceNoteFile = recordVoiceNote(project);
                if (voiceNoteFile != null) {
                    addVoiceNoteComment(editor, voiceNoteFile);
                }
            } catch (Exception ex) {
                Messages.showErrorDialog("Failed to record voice note: " + ex.getMessage(), "Error");
            }
        }
    }

    private File saveVoiceNote(File audioFile, Project project) throws IOException {
        String projectPath = project.getBasePath();
        if (projectPath == null) {
            throw new IOException("Project path not found.");
        }

        File voiceCommentsDir = new File(projectPath, "voicecomments");
        if (!voiceCommentsDir.exists()) {
            voiceCommentsDir.mkdir();
        }

        File savedFile = new File(voiceCommentsDir, "voice_note_" + System.currentTimeMillis() + ".wav");
        Files.copy(audioFile.toPath(), savedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        return savedFile;
    }


    private File recordVoiceNote(Project project) throws LineUnavailableException, IOException {
        File outputFile = saveVoiceNote(File.createTempFile("voice_note", ".wav"), project);

        AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("The audio system does not support the specified format.");
        }

        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        Thread recordingThread = new Thread(() -> {
            try (AudioInputStream ais = new AudioInputStream(line)) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outputFile);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        recordingThread.start();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        line.stop();
        line.close();

        Messages.showInfoMessage("Voice note saved as: " + outputFile.getAbsolutePath(), "Recording Complete");

        return outputFile;
    }

    private String getRelativePath(File file) {
        try {
            String projectPath = file.getParentFile().getParent();
            String absolutePath = file.getAbsolutePath();

            String relativePath = new File(projectPath).toURI().relativize(file.toURI()).getPath();

            return relativePath;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void addVoiceNoteComment(Editor editor, File voiceNoteFile) {
        Document document = editor.getDocument();
        int caretOffset = editor.getCaretModel().getOffset();

        String relativePath = getRelativePath(voiceNoteFile);

        String comment = "// [Voice Note: " + relativePath + "]\n";

        Project project = editor.getProject();
        if (project != null) {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                document.insertString(caretOffset, comment);
            });
        }
    }

}