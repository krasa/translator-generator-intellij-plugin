package krasa.translatorGenerator.action;

import static com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR;
import static com.intellij.openapi.actionSystem.CommonDataKeys.PSI_ELEMENT;

import krasa.translatorGenerator.Context;
import krasa.translatorGenerator.PsiFacade;
import krasa.translatorGenerator.assembler.TranslatorClassAssembler;
import krasa.translatorGenerator.assembler.TranslatorMethodAssembler;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;

public class TranslatorAction extends AnAction {

	@Override
	public void actionPerformed(AnActionEvent e) {
		PsiFacade psiFacade = new PsiFacade(e.getProject());
		PsiElement data = PSI_ELEMENT.getData(e.getDataContext());
		PsiElement psiElement = psiFacade.getPsiElement(e);

		PsiMethod psiMethod = getPsiMethod(psiElement);
		Context context = new Context(EDITOR.getData(e.getDataContext()));
		if (psiMethod != null) {
			PsiParameter[] parameters = psiMethod.getParameterList().getParameters();
			if (parameters.length == 1) {
				generateTranslatorMethod(psiMethod, psiFacade, context);
			}
		} else {
			PsiClass sourceClass = psiFacade.getPsiClassFromEvent(e);
			generatTranslatorClass(sourceClass, psiFacade, context);
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

	@Override
	public void update(AnActionEvent e) {
		PsiFacade psiFacade = new PsiFacade(e.getProject());
		PsiClass psiClass = psiFacade.getPsiClassFromEvent(e);
		e.getPresentation().setEnabled(psiClass != null);
	}

	private void generatTranslatorClass(final PsiClass sourceClass, final PsiFacade psiFacade, final Context context) {
		new WriteCommandAction.Simple(sourceClass.getProject()) {

			@Override
			protected void run() throws Throwable {
				new TranslatorClassAssembler(sourceClass, psiFacade, context).assemble();
			}
		}.execute();
	}
}
