package utils;

/**
 * 
 * @author Andrea Burattin
 *
 * @param <A>
 * @param <B>
 * @param <C>
 */
public class Triple<A, B, C> {

	private A first;
	private B second;
	private C third;
	
	public Triple(A first, B second, C third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}

	public A getFirst() {
		return first;
	}
	
	public void setFirst(A first) {
		this.first = first;
	}
	
	public B getSecond() {
		return second;
	}
	
	public void setSecond(B second) {
		this.second = second;
	}
	
	public C getThird() {
		return third;
	}
	
	public void setThird(C third) {
		this.third = third;
	}
	
	@Override
	public int hashCode() {
		int hash = 1;
		hash = hash * 17 + (first == null ? 0 : first.hashCode());
		hash = hash * 31 + (second == null ? 0 : second.hashCode());
		hash = hash * 13 + (third == null ? 0 : third.hashCode());
		return hash;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Triple<?, ?, ?>) {
			if (((Triple<?, ?, ?>) other).getFirst().equals(getFirst()) &&
				((Triple<?, ?, ?>) other).getSecond().equals(getSecond()) &&
				((Triple<?, ?, ?>) other).getThird().equals(getThird())) {
				return true;
			}
		}
		return false;
	}
}
