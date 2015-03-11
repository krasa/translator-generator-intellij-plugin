package krasa.translatorGenerator;

import krasa.translatorGenerator.generator.MethodCodeGenerator;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiUtil;

/**
 * @author Vojtech Krasa
 */
public class PsiBuilder {

	private Context context;
	private PsiElementFactory elementFactory;

	public PsiBuilder(Context context, Project project) {
		this.context = context;
		elementFactory = JavaPsiFacade.getElementFactory(project);
	}

	public PsiClass createTranslatorClass(PsiClass from, PsiClass to) {
		return elementFactory.createClassFromText(
				"public static class " + from.getName() + "To" + to.getName() + "Translator {}", null).getInnerClasses()[0];
	}

	public PsiMethod createTranslatorMethod(PsiClass builderClass, PsiClass from, PsiClass to) {
		PsiMethod methodFromText = elementFactory.createMethodFromText(
				new MethodCodeGenerator(from, to, context).translatorMethod(), builderClass);
		context.markTranslatorMethodProcessed(PsiUtil.getTypeByPsiElement(from), PsiUtil.getTypeByPsiElement(to));
		return methodFromText;
	}

	public PsiMethod createTranslatorMethod(PsiClass builderClass, PsiType fromType, PsiType toType) {
		PsiClassReferenceType from = (PsiClassReferenceType) fromType;
		PsiClassReferenceType to = (PsiClassReferenceType) toType;
		return createTranslatorMethod(builderClass, from.resolve(), to.resolve());
	}
}
