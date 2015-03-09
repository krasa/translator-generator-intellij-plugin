package krasa.translatorGenerator.action;

import krasa.translatorGenerator.PsiFacade;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.psi.PsiClass;

/**
 * @author Vojtech Krasa
 */
public abstract class TranslatorAction extends AnAction {

	@Override
	public void actionPerformed(AnActionEvent e) {
		PsiFacade psiFacade = new PsiFacade(e.getProject());
		execute(e, psiFacade);
	}

	protected abstract void execute(AnActionEvent e, PsiFacade psiFacade);

	@Override
	public void update(AnActionEvent e) {
		PsiFacade psiFacade = new PsiFacade(e.getProject());
		PsiClass psiClass = psiFacade.getPsiClassFromEvent(e);
		e.getPresentation().setEnabledAndVisible(psiClass != null);
	}

}
