package krasa.translatorGenerator.assembler;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.intellij.psi.PsiType;

/**
 * @author Vojtech Krasa
 */
public class TranslatorDto {

	private final PsiType from;
	private final PsiType to;
	public boolean processed;
	public boolean jaxbCollection;

	public TranslatorDto(PsiType from, PsiType to) {
		this.from = from;
		this.to = to;
	}

	public PsiType getFrom() {
		return from;
	}

	public PsiType getTo() {
		return to;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		TranslatorDto that = (TranslatorDto) o;

		if (from != null ? !from.equals(that.from) : that.from != null) {
			return false;
		}
		if (to != null ? !to.equals(that.to) : that.to != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = from != null ? from.getCanonicalText().hashCode() : 0;
		result = 31 * result + (to != null ? to.getCanonicalText().hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("from", from).append("to", to).append("processed", processed).toString();
	}

	public TranslatorDto jaxbCollection() {
		jaxbCollection = true;
		return this;
	}
}
