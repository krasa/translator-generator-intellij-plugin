package krasa.translatorGenerator;

import com.intellij.psi.PsiType;

//to be configurable
public class HACK {

	public static boolean shouldTranslate(String canonicalText) {
		return canonicalText.startsWith("com.t_motion") || canonicalText.startsWith("krasa");
	}

	static boolean isTranslationExcluded(PsiType getter) {
		return getter.getInternalCanonicalText().startsWith("javax.xml.bind");
	}
}
