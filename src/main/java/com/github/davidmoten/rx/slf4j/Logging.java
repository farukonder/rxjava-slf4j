package com.github.davidmoten.rx.slf4j;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Notification;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Functions;
import rx.subjects.PublishSubject;

public class Logging {

	private static final Logger DEFAULT_LOGGER = LoggerFactory
			.getILoggerFactory().getLogger(Logging.class.getName());

	public enum Level {
		INFO, WARN, DEBUG, TRACE, ERROR;
	}

	public static class Parameters<T> {

		private final Logger logger;
		private final String subscribedMessage;
		private final String unsubscribedMessage;
		private final Level subscribedLevel;
		private final Level unsubscribedLevel;
		private final PublishSubject<T> subject;
		private final Observable<Message<T>> observable;

		private Parameters(Logger logger, String subscribedMessage,
				String unsubscribedMessage, Level subscribedLevel,
				Level unsubscribedLevel, PublishSubject<T> subject,
				Observable<Message<T>> observable) {
			this.logger = logger;
			this.subscribedMessage = subscribedMessage;
			this.unsubscribedMessage = unsubscribedMessage;
			this.subscribedLevel = subscribedLevel;
			this.unsubscribedLevel = unsubscribedLevel;
			this.subject = subject;
			this.observable = observable;
		}

		public Logger getLogger() {
			return logger;
		}

		public Level getSubscribedLevel() {
			return subscribedLevel;
		}

		public String getSubscribedMessage() {
			return subscribedMessage;
		}

		public String getUnsubscribedMessage() {
			return unsubscribedMessage;
		}

		public Level getUnsubscribedLevel() {
			return unsubscribedLevel;
		}

		public Observable<Message<T>> getObservable() {
			return observable;
		}

		public PublishSubject<T> getSubject() {
			return subject;
		}

		public static <T> Builder<T> builder() {
			return new Builder<T>();
		}

		public static class Message<T> {
			private final Notification<T> value;
			private final String message;

			public Message(Notification<T> value, String message) {
				super();
				this.value = value;
				this.message = message;
			}

			public Notification<T> value() {
				return value;
			}

			public String message() {
				return message;
			}

			public Message<T> append(String s) {
				if (message.length() > 0)
					return new Message<T>(value, message + ", " + s);
				else
					return new Message<T>(value, message + s);
			}
		}

		public static class Builder<T> {

			private Logger logger;
			private String loggerName;
			private String onCompleteMessage = "onCompleted";
			private String subscribedMessage = "onSubscribe";
			private String unsubscribedMessage = "onUnsubscribe";
			private final boolean logOnNext = true;
			private final boolean logOnError = true;
			private String onErrorFormat = "";
			private String onNextFormat = "";
			private Level onNextLevel = Level.INFO;
			private Level onErrorLevel = Level.ERROR;
			private Level onCompletedLevel = Level.INFO;
			private Level subscribedLevel = Level.DEBUG;
			private Level unsubscribedLevel = Level.DEBUG;
			private Func1<? super T, ?> valueFunction = Functions
					.<T> identity();
			private boolean logStackTrace = false;
			private boolean logMemory = false;
			private final PublishSubject<T> subject = PublishSubject
					.<T> create();
			private Observable<Message<T>> observable = subject.materialize()
					.map(new Func1<Notification<T>, Message<T>>() {

						@Override
						public Message<T> call(Notification<T> n) {
							return new Message<T>(n, "");
						}
					});

			public Logger getLogger() {
				if (logger != null)
					return logger;
				else if (loggerName != null)
					return LoggerFactory.getLogger(loggerName);
				else {
					return DEFAULT_LOGGER;
				}
			}

			private Builder() {
			}

			/**
			 * Sets the SLF4J {@link Logger} to be used to do the logging.
			 * 
			 * @param logger
			 * @return
			 */
			public Builder<T> logger(Logger logger) {
				this.logger = logger;
				return this;
			}

			/**
			 * Sets the name of the {@link Logger} to used to do the logging. Is
			 * used as in {@link LoggerFactory#getLogger(String)}.
			 * 
			 * @param loggerName
			 * @return
			 */
			public Builder<T> name(String loggerName) {
				this.loggerName = loggerName;
				return this;
			}

			private Builder<T> source() {
				StackTraceElement[] elements = Thread.currentThread()
						.getStackTrace();
				String callingClassName = elements[4].getClassName();
				return name(callingClassName);
			}

			/**
			 * Sets the cls to be used to create a {@link Logger} to do the
			 * logging. It is used as in {@link LoggerFactory#getLogger(Class)}.
			 * 
			 * @param cls
			 * @return
			 */
			public Builder<T> logger(Class<?> cls) {
				return name(cls.getName());
			}

			/**
			 * The message to be logged on stream completion.
			 * 
			 * @param onCompleteMessage
			 * @return
			 */
			public Builder<T> onCompleted(final String onCompleteMessage) {
				this.onCompleteMessage = onCompleteMessage;
				return this;
			}

			/**
			 * The message to be logged when subscribed.
			 * 
			 * @param subscribedMessage
			 * @return
			 */
			public Builder<T> subscribed(String subscribedMessage) {
				this.subscribedMessage = subscribedMessage;
				return this;
			}

			/**
			 * The message to be logged when unsubscribed.
			 * 
			 * @param unsubscribedMessage
			 * @return
			 */
			public Builder<T> unsubscribed(String unsubscribedMessage) {
				this.unsubscribedMessage = unsubscribedMessage;
				return this;
			}

			public Builder<T> onNext(final boolean logOnNext) {
				this.observable = observable
						.filter(new Func1<Message<T>, Boolean>() {

							@Override
							public Boolean call(Message<T> m) {
								return m.value().isOnNext() == logOnNext;
							}
						});
				return this;
			}

			public Builder<T> onError(final boolean logOnError) {
				this.observable = observable
						.filter(new Func1<Message<T>, Boolean>() {

							@Override
							public Boolean call(Message<T> m) {
								return m.value().isOnError() == logOnError;
							}
						});
				return this;
			}

			public Builder<T> onErrorPrefix(String onErrorPrefix) {
				this.onErrorFormat = onErrorPrefix + "%s";
				return this;
			}

			public Builder<T> onErrorFormat(String onErrorFormat) {
				this.onErrorFormat = onErrorFormat;
				return this;
			}

			public Builder<T> onNextPrefix(String onNextPrefix) {
				this.onNextFormat = onNextPrefix + "%s";
				return this;
			}

			public Builder<T> onNextFormat(String onNextFormat) {
				this.onNextFormat = onNextFormat;
				return this;
			}

			public Builder<T> onNext(Level onNextLevel) {
				this.onNextLevel = onNextLevel;
				return this;
			}

			public Builder<T> onError(Level onErrorLevel) {
				this.onErrorLevel = onErrorLevel;
				return this;
			}

			public Builder<T> onCompleted(Level onCompletedLevel) {
				this.onCompletedLevel = onCompletedLevel;
				return this;
			}

			public Builder<T> subscribed(Level subscribedLevel) {
				this.subscribedLevel = subscribedLevel;
				return this;
			}

			public Builder<T> prefix(String prefix) {
				onNextPrefix(prefix);
				return onErrorPrefix(prefix);
			}

			public Builder<T> unsubscribed(Level unsubscribedLevel) {
				this.unsubscribedLevel = unsubscribedLevel;
				return this;
			}

			public Builder<T> showCount(final String label) {
				observable = observable
						.map(new Func1<Message<T>, Message<T>>() {
							AtomicLong count = new AtomicLong(0);

							@Override
							public Message<T> call(Message<T> m) {
								long val;
								if (m.value().isOnNext())
									val = count.incrementAndGet();
								else
									val = count.get();

								return m.append(label + "=" + val);
							}
						});
				return this;
			}

			public Builder<T> showCount() {
				return showCount("count");
			}

			public Builder<T> every(final int every) {
				if (every > 1)
					observable = observable
							.filter(new Func1<Message<T>, Boolean>() {
								AtomicLong count = new AtomicLong(0);

								@Override
								public Boolean call(Message<T> t) {
									if (t.value().isOnNext())
										return count.incrementAndGet() % every == 0;
									else
										return true;
								}
							});
				return this;
			}

			public Builder<T> showValue(boolean logValue) {
				if (logValue)
					return showValue();
				else
					return excludeValue();
			}

			public Builder<T> showValue() {
				if (onNextFormat.length() == 0)
					onNextFormat = "%s";
				return this;
			}

			public Builder<T> value(Func1<? super T, ?> function) {
				this.valueFunction = function;
				return this;
			}

			public Builder<T> excludeValue() {
				onNextFormat = "";
				return this;
			}

			public Builder<T> showStackTrace() {
				this.logStackTrace = true;
				return this;
			}

			public Builder<T> when(final Func1<? super T, Boolean> when) {
				observable = observable
						.filter(new Func1<Message<T>, Boolean>() {
							@Override
							public Boolean call(Message<T> t) {
								if (t.value().isOnNext())
									return when.call(t.value().getValue());
								else
									return true;
							}
						});
				return this;
			}

			public Builder<T> start(final long start) {
				observable = observable
						.filter(new Func1<Message<T>, Boolean>() {
							AtomicLong count = new AtomicLong(0);

							@Override
							public Boolean call(Message<T> t) {
								if (t.value().isOnNext())
									return start <= count.incrementAndGet();
								else
									return true;
							}
						});
				return this;
			}

			public Builder<T> finish(final long finish) {
				observable = observable
						.filter(new Func1<Message<T>, Boolean>() {
							AtomicLong count = new AtomicLong(0);

							@Override
							public Boolean call(Message<T> t) {
								if (t.value().isOnNext())
									return finish >= count.incrementAndGet();
								else
									return true;
							}
						});
				return this;
			}

			public Builder<T> to(
					Func1<Observable<? super Message<T>>, Observable<Message<T>>> f) {
				this.observable = f.call(observable);
				return this;
			}

			public Builder<T> showMemory() {
				logMemory = true;
				return this;
			}

			public OperatorLogging<T> log() {
				return new OperatorLogging<T>(new Parameters<T>(getLogger(),
						subscribedMessage, unsubscribedMessage,
						subscribedLevel, unsubscribedLevel, subject,
						observable.doOnNext(log)));
			}

			private final Action1<Message<T>> log = new Action1<Message<T>>() {

				@Override
				public void call(Message<T> m) {

					if (m.value().isOnCompleted() && onCompleteMessage != null) {
						StringBuilder s = new StringBuilder();
						addDelimited(s, onCompleteMessage);
						addDelimited(s, m.message());
						addMemory(s);
						Logging.log(getLogger(), s.toString(),
								onCompletedLevel, null);
					} else if (m.value().isOnError() && logOnError) {
						StringBuilder s = new StringBuilder();
						addDelimited(
								s,
								String.format(onErrorFormat, m.value()
										.getThrowable().getMessage()));
						addDelimited(s, m.message());
						addMemory(s);
						Logging.log(getLogger(), s.toString(), onErrorLevel, m
								.value().getThrowable());
					} else if (m.value().isOnNext() && logOnNext) {
						StringBuilder s = new StringBuilder();
						if (onNextFormat.length() > 0)
							s.append(String.format(onNextFormat, String
									.valueOf(valueFunction.call(m.value()
											.getValue()))));
						addDelimited(s, m.message());
						addMemory(s);
						addStackTrace(s);
						Logging.log(getLogger(), s.toString(), onNextLevel,
								null);
					}
				}

				private void addStackTrace(StringBuilder s) {
					if (logStackTrace) {
						for (StackTraceElement elem : Thread.currentThread()
								.getStackTrace()) {
							s.append("\n    ");
							s.append(elem);
						}
					}
				}

				private void addMemory(StringBuilder s) {
					if (logMemory)
						addDelimited(s, memoryUsage());
				}

			};
		}
	}

	private static void delimiter(StringBuilder s) {
		if (s.length() > 0)
			s.append(", ");
	}

	private static void addDelimited(StringBuilder b, String s) {
		if (s.length() > 0) {
			delimiter(b);
			b.append(s);
		}
	}

	private static String memoryUsage() {
		StringBuilder s = new StringBuilder();
		Runtime r = Runtime.getRuntime();
		long mem = r.totalMemory() - r.freeMemory();
		s.append("usedMem=");
		s.append(new DecimalFormat("0").format(mem / 1000000.0));
		s.append("MB, percentMax=");
		s.append(new DecimalFormat("0.0").format((double) mem / r.maxMemory()));
		return s.toString();
	}

	public static <T> OperatorLogging<T> log() {
		return Logging.<T> logger().showValue().log();
	}

	public static <T> Parameters.Builder<T> logger() {
		return Parameters.<T> builder().source();
	}

	public static <T> Parameters.Builder<T> logger(String name) {
		return Parameters.<T> builder().name(name);
	}

	public static <T> Parameters.Builder<T> logger(Logger logger) {
		return Parameters.<T> builder().logger(logger);
	}

	public static <T> Parameters.Builder<T> logger(Class<?> cls) {
		return Parameters.<T> builder().logger(cls);
	}

	public static void log(Logger logger, String msg, Level level, Throwable t) {

		if (t == null) {
			if (level == Level.INFO)
				logger.info(msg);
			else if (level == Level.DEBUG)
				logger.debug(msg);
			else if (level == Level.WARN)
				logger.warn(msg);
			else if (level == Level.TRACE)
				logger.trace(msg);
			else if (level == Level.ERROR)
				logger.error(msg);
		} else {
			if (level == Level.INFO)
				logger.info(msg, t);
			else if (level == Level.DEBUG)
				logger.debug(msg, t);
			else if (level == Level.WARN)
				logger.warn(msg, t);
			else if (level == Level.TRACE)
				logger.trace(msg, t);
			else if (level == Level.ERROR)
				logger.error(msg, t);
		}
	}
}