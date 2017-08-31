package krasa.translatorGenerator;

import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PropertyUtil;

/**
 * @author Vojtech Krasa
 */
public class Utils {
	private static final Logger LOG = Logger.getInstance("#" + Utils.class.getName());

	private static String camelCase(String s) {
		return toLowerCase(s.charAt(0)) + s.substring(1);
	}

	/** isBoolean must return boolean, not Boolean */
	public static PsiMethod getter(PsiField field) {
		if (field == null) {
			return null;
		}
		String name = field.getName();
		List<PsiMethod> getters = PropertyUtil.getGetters(field.getContainingClass(), name);
		if (getters.size() == 1) {
			return getters.get(0);
		}
		name = normalize(name);

		getters = PropertyUtil.getGetters(field.getContainingClass(), name);
		if (getters.size() == 1) {
			return getters.get(0);
		}
		return null;
	}

	public static PsiMethod setter(PsiField field) {
		String name = field.getName();
		List<PsiMethod> setters = PropertyUtil.getSetters(field.getContainingClass(), name);
		if (setters.size() == 1) {
			return setters.get(0);
		}

		name = normalize(name);

		setters = PropertyUtil.getSetters(field.getContainingClass(), name);
		if (setters.size() == 1) {
			return setters.get(0);
		}
		return null;
	}

	/** Integer _int; has #setInt */
	@NotNull
	private static String normalize(String name) {
		if (!StringUtils.isAlphanumeric(name)) {
			int sz = name.length();
			for (int i = 0; i < sz; ++i) {
				if (Character.isLetterOrDigit(name.charAt(i))) {
					name = StringUtils.substring(name, i, sz);
					break;
				}
			}

		}
		return name;
	}

	public static String capitalize(String fieldName) {
		return toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
	}

	public static boolean isStatic(PsiField field) {
		return field.getModifierList() != null && field.getModifierList().hasExplicitModifier("static");
	}

	public static String getPresentableFullType(PsiType type) {
		PsiClassType fromGetterType = (PsiClassType) type;
		return fromGetterType.getPresentableText().replaceAll("[<>, ]", "");
	}

	@NotNull
	public static String todo() {
		LOG.warn(new RuntimeException());
		return "//TODO ";
	}

	@NotNull
	public static String newLineTodo(AtomicBoolean containsTODO) {
		if (!containsTODO.getAndSet(true)) {
			return newLineTodo();
		}
		return "";
	}

	@NotNull
	public static String newLineTodo() {
		LOG.warn(new RuntimeException());
		return "\n//TODO ";
	}
}