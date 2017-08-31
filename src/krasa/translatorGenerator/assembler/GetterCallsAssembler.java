package krasa.translatorGenerator.assembler;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;

import krasa.translatorGenerator.Context;
import krasa.translatorGenerator.PsiFacade;
import krasa.translatorGenerator.generator.GetterCallsGenerator;

/**
 * @author Vojtech Krasa
 */
public class GetterCallsAssembler extends Assembler {

	private final PsiLocalVariable localVariable;
	private final PsiFacade psiFacade;
	private final Context context;

	public GetterCallsAssembler(PsiLocalVariable variable, PsiFacade psiFacade, Context context) {
		super(psiFacade, context);
		this.localVariable = variable;
		this.psiFacade = psiFacade;
		this.context = context;
	}

	public void generateGetterCalls() {
		GetterCallsGenerator generator = new GetterCallsGenerator(localVariable, context);
		PsiDeclarationStatement statement = PsiTreeUtil.getTopmostParentOfType(localVariable, PsiDeclarationStatement.class);

		StringBuilder sb = new StringBuilder();
		for (String s1 : generator.generateGetterCalls()) {
			sb.append(s1);
		}

		PsiDocumentManager instance = PsiDocumentManager.getInstance(psiFacade.getProject());
		Document document = instance.getDocument(localVariable.getContainingFile());
		if (document == null) {
			return;
		}
		int i = statement.getNextSibling().getTextOffset();
		document.replaceString(i, i, sb);
		instance.commitDocument(document);

		PsiFile psiFile = instance.getPsiFile(document);
		if (psiFile != null) {
			CodeStyleManager.getInstance(project).reformat(psiFile);
		}
	}

}
