package com.github.microprograms.micro_api_sdk.model;

import java.io.Serializable;

public class PlainEntityRefDefinition implements Serializable {
	private static final long serialVersionUID = 1L;

	private PlainEntityRefItem source;
	private PlainEntityRefItem target;

	public PlainEntityRefItem getSource() {
		return source;
	}

	public void setSource(PlainEntityRefItem source) {
		this.source = source;
	}

	public PlainEntityRefItem getTarget() {
		return target;
	}

	public void setTarget(PlainEntityRefItem target) {
		this.target = target;
	}

	public static class PlainEntityRefItem implements Serializable {
		private static final long serialVersionUID = 1L;

		/**
		 * 唯一标识符
		 */
		private String name;
		private PlainEntityRefMockConfig mock;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public PlainEntityRefMockConfig getMock() {
			return mock;
		}

		public void setMock(PlainEntityRefMockConfig mock) {
			this.mock = mock;
		}

		public static class PlainEntityRefMockConfig implements Serializable {
			private static final long serialVersionUID = 1L;

			/**
			 * 每个实例最小重复次数
			 */
			public final static int default_minRepeatPerInstance = 0;
			/**
			 * 每个实例最大重复次数
			 */
			public final static int default_maxRepeatPerInstance = 5;

			/**
			 * 每个实例最小重复次数
			 */
			private int minRepeatPerInstance = default_minRepeatPerInstance;
			/**
			 * 每个实例最大重复次数
			 */
			private int maxRepeatPerInstance = default_maxRepeatPerInstance;

			public int getMinRepeatPerInstance() {
				return minRepeatPerInstance;
			}

			public void setMinRepeatPerInstance(int minRepeatPerInstance) {
				this.minRepeatPerInstance = minRepeatPerInstance;
			}

			public int getMaxRepeatPerInstance() {
				return maxRepeatPerInstance;
			}

			public void setMaxRepeatPerInstance(int maxRepeatPerInstance) {
				this.maxRepeatPerInstance = maxRepeatPerInstance;
			}
		}
	}
}
