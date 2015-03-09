package krasa.translatorGenerator.action;

import static com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR;

import krasa.translatorGenerator.Context;
import krasa.translatorGenerator.PsiFacade;
import krasa.translatorGenerator.assembler.TranslatorClassAssembler;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiClass;

/**
 * @author Vojtech Krasa
 */
public class GenerateTranslatorClassAction extends TranslatorAction {

	protected void execute(final AnActionEvent e, final PsiFacade psiFacade) {
		final PsiClass sourceClass = psiFacade.getPsiClassFromEvent(e);
		new WriteCommandAction.Simple(sourceClass.getProject()) {

			@Override
			protected void run() throws Throwable {
				Context context = new Context(e.getProject(), EDITOR.getData(e.getDataContext()));
				new TranslatorClassAssembler(sourceClass, psiFacade, context).assemble();
			}
		}.execute();
	}

}
