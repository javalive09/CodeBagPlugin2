package com.javalive09.codebag;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.OutputListener;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.icons.AllIcons;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NotNull;
import sun.java2d.loops.ProcessPath;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class Plugin extends AnAction {

    private String methodName;
    private String filePathName;
    private static final String ACTION = "com.javalive09.ACTION_CODEBAG";
    private static final String ANNOTATION = "@Run";
    private static final String SPLIT = "src/main/java/";

    private static void log(String msg) {
        PluginManager.getLogger().info(msg);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
            String projectPath = project.getBasePath();
            log("actionPerformed ====== projectPath");

            int index = filePathName.indexOf(SPLIT);
            String classNamePath = filePathName.substring(index + SPLIT.length(), filePathName.length() - 5);
            String className = classNamePath.replace("/", ".");
            String cmd = "am start -a " + ACTION + " --es className " + className
                    + " --es methodName " + methodName;

            ArrayList<String> cmds = new ArrayList<>();
            cmds.add("./gradlew");
            cmds.add("runMethod");
            ArrayList<String> params = new ArrayList<>();
            params.add("-Dclass_method_name=" + className + "$" + methodName);
            GeneralCommandLine generalCommandLine = new GeneralCommandLine(cmds);
            generalCommandLine.addParameters(params);
            generalCommandLine.setCharset(StandardCharsets.UTF_8);
            generalCommandLine.setWorkDirectory(project.getBasePath());
            StringBuilder stringBuilder = new StringBuilder();
            int c = 0;
            try {
                c = runProcess(generalCommandLine, stringBuilder);
            } catch (ExecutionException executionException) {
                executionException.printStackTrace();
            } finally {
                log("exitCode ====== " + c );
            }


        }
    }

    private static int runProcess(GeneralCommandLine commandLine, StringBuilder executionLog) throws ExecutionException {
        OSProcessHandler handler =
                new OSProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString(), commandLine.getCharset());
        StringBuilder outContent = new StringBuilder();
        StringBuilder errContent = new StringBuilder();
        handler.addProcessListener(new OutputListener(outContent, errContent));
        handler.startNotify();
        handler.waitFor();
        int exitCode = handler.getProcess().exitValue();
        executionLog.append("Return code: ").append(errContent);
        return exitCode;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        String method = getMethodName(e, ANNOTATION);
        if (method != null && method.length() > 0) {
            e.getPresentation().setVisible(true);
            e.getPresentation().setText("run > " + method + "()");
        } else {
            e.getPresentation().setVisible(false);
        }
        methodName = method;
    }

    private String getMethodName(AnActionEvent e, String annotationName) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        Project project = e.getProject();
        if (editor != null) {
            final SelectionModel selectionModel = editor.getSelectionModel();
            String selectText = selectionModel.getSelectedText();
            if (project != null) {
                PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
                if (psiFile != null) {
                    filePathName = psiFile.getVirtualFile().getPath();
                    log("filePathName=" + filePathName);
                    PsiElement[] psiElements = psiFile.getChildren();
                    for (PsiElement psiElement : psiElements) {
                        if (psiElement instanceof PsiClass) {
                            PsiClass psiClass = (PsiClass) psiElement;
                            for (PsiMethod method : psiClass.getMethods()) {
                                if (method.getName().equals(selectText)) {
                                    PsiModifierList psiModifierList = method.getModifierList();
                                    PsiAnnotation[] annotations = psiModifierList.getAnnotations();
                                    for (PsiAnnotation annotation : annotations) {
                                        if (annotation.getText().contains(annotationName)) {
                                            return selectText;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

}