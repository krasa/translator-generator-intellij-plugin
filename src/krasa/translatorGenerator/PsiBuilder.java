package krasa.translatorGenerator;

import krasa.translatorGenerator.generator.MethodCodeGenerator;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiMethod;

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

	public PsiClass createTranslatorClass(PsiClass from, final PsiClass to) {
		return elementFactory.createClassFromText(
				"public static class " + from.getName() + "To" + to.getName() + "Translator {}", null).getInnerClasses()[0];
	}

	public PsiMethod createTranslatorMethod(PsiClass builderClass, PsiClass from, PsiClass to) {
		return elementFactory.createMethodFromText(new MethodCodeGenerator(from, to, context).translatorMethod(),
				builderClass);
	}

}
