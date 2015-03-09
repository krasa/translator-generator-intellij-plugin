package krasa.translatorGenerator.assembler;

import krasa.translatorGenerator.Context;
import krasa.translatorGenerator.PsiFacade;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiUtil;

/**
 * @author Vojtech Krasa
 */
public class TranslatorMethodAssembler extends Assembler {

	private static final Logger LOG = Logger.getInstance(TranslatorMethodAssembler.class.getName());

	private PsiMethod psiMethod;

	public TranslatorMethodAssembler(PsiMethod psiMethod, PsiFacade psiFacade, Context context) {
		super(psiFacade, context);
		this.psiMethod = psiMethod;
	}

	public void assemble() {
		PsiClass topLevelClass = PsiUtil.getTopLevelClass(psiMethod);
		PsiParameter[] parameters = psiMethod.getParameterList().getParameters();
		PsiType toType = psiMethod.getReturnType();
		PsiClass to = getPsiClass(toType);
		PsiType fromType = parameters[0].getType();
		PsiClass from = getPsiClass(fromType);

		PsiMethod translatorMethod = psiBuilder.createTranslatorMethod(topLevelClass, from, to);
		context.processedTranslator(fromType, toType);
		try {
			psiMethod.replace(translatorMethod);
			psiFacade.shortenClassReferences(translatorMethod);
			psiFacade.reformat(translatorMethod);
			generateScheduledTranslators(topLevelClass);
		} catch (Throwable e) {
			throw new RuntimeException("topLevelClass=" + topLevelClass + ", from=" + from + ", to=" + to
					+ ", translatorMethod=" + translatorMethod.getName(), e);
		}
	}

	private PsiClass getPsiClass(PsiType type) {
		PsiClassReferenceType returnType = (PsiClassReferenceType) type;
		PsiJavaCodeReferenceElement reference = returnType.getReference();
		return (PsiClass) reference.resolve();
	}
}
