package krasa.translatorGenerator.action;

import static com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;

import krasa.translatorGenerator.Context;
import krasa.translatorGenerator.PsiFacade;
import krasa.translatorGenerator.SettingsUI;
import krasa.translatorGenerator.assembler.TranslatorMethodAssembler;

/**
 * @author Vojtech Krasa
 */
public class GenerateTranslatorMethodAction extends TranslatorAction {

	protected void execute(AnActionEvent e, PsiFacade psiFacade) {
		PsiElement psiElement = psiFacade.getPsiElement(e);
		PsiMethod psiMethod = getPsiMethod(psiElement);
		Context context = new Context(e.getProject(), EDITOR.getData(e.getDataContext()));

		if (psiMethod != null) {
			boolean b = SettingsUI.showDialog(getEventProject(e), psiMethod);
			if (b) {
				generateTranslatorMethod(psiMethod, psiFacade, context);
			}
		}
	}

	private void generateTranslatorMethod(final PsiMethod psiMethod, final PsiFacade psiFacade, final Context context) {
		new WriteCommandAction.Simple(psiMethod.getProject()) {

			@Override
			protected void run() throws Throwable {
				new TranslatorMethodAssembler(psiMethod, psiFacade, context).assemble();
			}
		}.execute();
	}

	@Override
	public void update(AnActionEvent e) {
		super.update(e);

		PsiFacade psiFacade = new PsiFacade(e.getProject());
		PsiElement psiElement = psiFacade.getPsiElement(e);
		PsiMethod psiMethod = getPsiMethod(psiElement);

		boolean oneToOne = psiMethod != null && psiMethod.getParameterList().getParametersCount() == 1 && !PsiType.VOID.equals(psiMethod.getReturnType());
		boolean twoParameters = psiMethod != null && psiMethod.getParameterList().getParametersCount() == 2 && PsiType.VOID.equals(psiMethod.getReturnType());
		e.getPresentation().setEnabledAndVisible(oneToOne || twoParameters);
	}

	private PsiMethod getPsiMethod(PsiElement psiElement) {
		if (psiElement != null) {
			PsiElement parent = psiElement.getParent();
			if (parent != null) {
				if (parent instanceof PsiMethod) {
					return (PsiMethod) parent;
				} else {
					return getPsiMethod(parent);
				}
			}
		}
		return null;
	}
}
