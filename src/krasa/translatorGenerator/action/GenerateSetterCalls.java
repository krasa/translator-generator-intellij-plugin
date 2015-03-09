package krasa.translatorGenerator.action;

import static com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR;

import krasa.translatorGenerator.Context;
import krasa.translatorGenerator.PsiFacade;
import krasa.translatorGenerator.assembler.SetterCallsAssembler;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author Vojtech Krasa
 */
public class GenerateSetterCalls extends AnAction {

	@Override
	public void actionPerformed(AnActionEvent e) {
		PsiFacade psiFacade = new PsiFacade(e.getProject());
		PsiElement psiElement = psiFacade.getPsiElement(e);
		PsiLocalVariable psiMethod = PsiTreeUtil.getTopmostParentOfType(psiElement, PsiLocalVariable.class);
		Context context = new Context(e.getProject(), EDITOR.getData(e.getDataContext()));
		if (psiMethod != null) {
			generateSetterCalls(psiMethod, psiFacade, context);
		}

	}

	private void generateSetterCalls(final PsiLocalVariable psiReferenceExpression, final PsiFacade psiFacade,
			final Context context) {
		new WriteCommandAction.Simple(psiReferenceExpression.getProject()) {

			@Override
			protected void run() throws Throwable {
				new SetterCallsAssembler(psiReferenceExpression, psiFacade, context).generateSetterCalls();
			}
		}.execute();

	}

	@Override
	public void update(AnActionEvent e) {
		PsiFacade psiFacade = new PsiFacade(e.getProject());
		PsiElement psiElement = psiFacade.getPsiElement(e);
		PsiLocalVariable variable = PsiTreeUtil.getParentOfType(psiElement, PsiLocalVariable.class);
		e.getPresentation().setEnabled(variable != null);
	}

}
