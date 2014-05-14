package org.mtc.conform.math;

public class ParamHolder {
	ParamHolder() {
		m_params = new Complex[s_max_params];
		m_paramInvs = new Complex[s_max_params];
	}
	public final static int s_max_params = 6;
	private int m_numParams;
	private final Complex[] m_params;
	private final Complex[] m_paramInvs;
}
