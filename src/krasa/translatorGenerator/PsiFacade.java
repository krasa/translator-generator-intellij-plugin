package krasa.translatorGenerator;

import static com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR;
import static com.intellij.openapi.actionSystem.CommonDataKeys.PSI_FILE;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author Vojtech Krasa
 */
public class PsiFacade {

	private Project project;

	public PsiFacade(Project project) {
		this.project = project;
	}

	public Project getProject() {
		return project;
	}

	public PsiClass getPsiClassFromEvent(AnActionEvent e) {
		PsiFile file = e.getData(PSI_FILE);
		Editor editor = e.getData(EDITOR);
		if (file == null || editor == null) {
			return null;
		}
		int offset = editor.getCaretModel().getOffset();
		PsiElement element = file.findElementAt(offset);
		return PsiTreeUtil.getParentOfType(element, PsiClass.class);
	}

	public PsiElement getPsiElement(AnActionEvent e) {
		PsiFile file = e.getData(PSI_FILE);
		Editor editor = e.getData(EDITOR);
		if (file == null || editor == null) {
			return null;
		}
		int offset = editor.getCaretModel().getOffset();
		PsiElement element = file.findElementAt(offset);
		return element;
	}

	public PsiField createFieldFromText(String fieldText, PsiClass targetClass) {
		return JavaPsiFacade.getElementFactory(project).createFieldFromText(fieldText, targetClass);
	}

	public PsiElement shortenClassReferences(PsiElement psiClass) {
		return JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiClass);
	}

	public PsiElement reformat(PsiElement psiClass) {
		return CodeStyleManager.getInstance(project).reformat(psiClass);
	}
}
