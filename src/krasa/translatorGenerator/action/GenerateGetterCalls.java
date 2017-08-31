package krasa.translatorGenerator.action;

import static com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.util.PsiTreeUtil;

import krasa.translatorGenerator.Context;
import krasa.translatorGenerator.PsiFacade;
import krasa.translatorGenerator.assembler.GetterCallsAssembler;

/**
 * @author Vojtech Krasa
 */
public class GenerateGetterCalls extends AnAction {

	@Override
	public void actionPerformed(AnActionEvent e) {
		PsiFacade psiFacade = new PsiFacade(e.getProject());
		PsiElement psiElement = psiFacade.getPsiElement(e);
		PsiLocalVariable psiMethod = PsiTreeUtil.getTopmostParentOfType(psiElement, PsiLocalVariable.class);
		Context context = new Context(e.getProject(), EDITOR.getData(e.getDataContext()));
		if (psiMethod != null) {
			generateGetterCalls(psiMethod, psiFacade, context);
		}

	}

	private void generateGetterCalls(final PsiLocalVariable psiReferenceExpression, final PsiFacade psiFacade, final Context context) {
		new WriteCommandAction.Simple(psiReferenceExpression.getProject()) {

			@Override
			protected void run() throws Throwable {
				new GetterCallsAssembler(psiReferenceExpression, psiFacade, context).generateGetterCalls();
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
