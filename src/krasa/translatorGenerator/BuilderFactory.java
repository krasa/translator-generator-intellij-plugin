package krasa.translatorGenerator;

import krasa.translatorGenerator.generator.MethodCodeGenerator;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

public class BuilderFactory {

	private PsiFacade psiFacade;
	private Context context;

	public BuilderFactory(PsiFacade psiFacade, Context context) {
		this.psiFacade = psiFacade;
		this.context = context;
	}

	public PsiClass createTranslatorClass(PsiClass from, final PsiClass to) {
		return psiFacade.createClassFromText("public static class " + from.getName() + "To" + to.getName()
				+ "Translator {}");
	}

	public PsiMethod createTranslatorMethod(PsiClass builderClass, PsiClass from, PsiClass to) {
		return psiFacade.createMethodFromText(new MethodCodeGenerator(from, to, context).translatorMethod(),
				builderClass);
	}

}
