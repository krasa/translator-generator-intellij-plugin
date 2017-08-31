package krasa.translatorGenerator;

import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.psi.PsiType;
import com.intellij.util.xmlb.XmlSerializerUtil;

@State(name = "krasa.translatorGenerator.GlobalSettings", storages = { @Storage(id = "translatorGenerator", file = "$APP_CONFIG$/translatorGenerator.xml") })
public class GlobalSettings implements ApplicationComponent, PersistentStateComponent<GlobalSettings> {
	Set<String> included = new HashSet<String>();
	Set<String> excluded = new HashSet<String>();

	public GlobalSettings() {
		excluded.add("javax.xml.bind");
	}

	public boolean shouldTranslate(String canonicalText) {
		for (String s : included) {
			if (canonicalText.startsWith(s)) {
				return true;
			}
		}
		return false;
	}

	public boolean isTranslationExcluded(PsiType getter) {
		String internalCanonicalText = getter.getInternalCanonicalText();
		for (String s : excluded) {
			if (internalCanonicalText.startsWith(s)) {
				return true;
			}
		}
		return false;
	}

	public static GlobalSettings getInstance() {
		return ApplicationManager.getApplication().getComponent(GlobalSettings.class);
	}

	@Nullable
	@Override
	public GlobalSettings getState() {
		return this;
	}

	@Override
	public void loadState(GlobalSettings globalSettings) {
		XmlSerializerUtil.copyBean(globalSettings, this);
	}

	public Set<String> getIncluded() {
		return included;
	}

	public void setIncluded(Set<String> included) {
		this.included = included;
	}

	public Set<String> getExcluded() {
		return excluded;
	}

	public void setExcluded(Set<String> excluded) {
		this.excluded = excluded;
	}

}
