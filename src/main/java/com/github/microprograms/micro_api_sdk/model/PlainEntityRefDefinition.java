package com.github.microprograms.micro_api_sdk.model;

import java.io.Serializable;

public class PlainEntityRefDefinition implements Serializable {
	private static final long serialVersionUID = 1L;

	private Entity source;
	private Entity target;

	public Entity getSource() {
		return source;
	}

	public void setSource(Entity source) {
		this.source = source;
	}

	public Entity getTarget() {
		return target;
	}

	public void setTarget(Entity target) {
		this.target = target;
	}

	public static class Entity implements Serializable {
		private static final long serialVersionUID = 1L;

		/**
		 * 唯一标识符
		 */
		private String name;
		private Mock mock;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Mock getMock() {
			return mock;
		}

		public void setMock(Mock mock) {
			this.mock = mock;
		}

		public static class Mock implements Serializable {
			private static final long serialVersionUID = 1L;

			/**
			 * 每个实例最小重复次数
			 */
			private int minRepeatPerInstance;
			/**
			 * 每个实例最大重复次数
			 */
			private int maxRepeatPerInstance;

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
