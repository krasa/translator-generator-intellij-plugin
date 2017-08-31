package krasa.translatorGenerator;

import static com.siyeh.ig.psiutils.CollectionUtils.isCollectionClassOrInterface;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiUtil;

import krasa.translatorGenerator.generator.ArrayTranslator;
import krasa.translatorGenerator.generator.CollectionTranslator;
import krasa.translatorGenerator.generator.MethodCodeGenerator;

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
		return elementFactory.createClassFromText("public static class " + from.getName() + "To" + to.getName() + "Translator {}", null).getInnerClasses()[0];
	}

	public PsiMethod createTranslatorMethod(PsiClass builderClass, PsiType fromType, PsiType toType) {
		if (fromType instanceof PsiArrayType) {
			fromType = fromType.getDeepComponentType();
			toType = toType.getDeepComponentType();
			PsiClassType from = (PsiClassType) fromType;
			PsiClassType to = (PsiClassType) toType;
			return createArrayTranslatorMethod(builderClass, from.resolve(), to.resolve());
		}

		if (isCollectionClassOrInterface(fromType) && isCollectionClassOrInterface(toType)) {
			PsiClassType from = (PsiClassReferenceType) fromType;
			PsiClassType to = (PsiClassReferenceType) toType;
			return createCollectionTranslatorMethod(builderClass, from, to);
		}

		PsiClassType from = (PsiClassType) fromType;
		PsiClassType to = (PsiClassType) toType;
		return createTranslatorMethod(builderClass, from.resolve(), to.resolve());
	}

	private PsiMethod createCollectionTranslatorMethod(PsiClass builderClass, PsiClassType from, PsiClassType to) {
		PsiMethod methodFromText = elementFactory.createMethodFromText(new CollectionTranslator(from, to, context).translatorMethod(), builderClass);
		context.markTranslatorMethodProcessed(from, to);
		return methodFromText;
	}

	public PsiMethod createTranslatorMethod(PsiClass builderClass, PsiClass from, PsiClass to) {
		PsiMethod methodFromText = elementFactory.createMethodFromText(new MethodCodeGenerator(from, to, context).translatorMethod(), builderClass);
		context.markTranslatorMethodProcessed(PsiUtil.getTypeByPsiElement(from), PsiUtil.getTypeByPsiElement(to));
		return methodFromText;
	}

	private PsiMethod createArrayTranslatorMethod(PsiClass builderClass, PsiClass from, PsiClass to) {
		PsiMethod methodFromText = elementFactory.createMethodFromText(new ArrayTranslator(from, to, context).arrayTranslatorMethod(), builderClass);
		context.markTranslatorMethodProcessed(PsiUtil.getTypeByPsiElement(from), PsiUtil.getTypeByPsiElement(to));
		return methodFromText;
	}
}
