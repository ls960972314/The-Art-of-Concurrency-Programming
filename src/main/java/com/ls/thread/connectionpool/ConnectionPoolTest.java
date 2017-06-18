package com.ls.thread.connectionpool;

import java.sql.Connection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * @author lishun
 * @since 2017年6月18日 下午11:13:45
 */
public class ConnectionPoolTest {
	
	static ConnectionPool pool = new ConnectionPool(10);
	
	// 保证所有ConnectionRunner能够同时开始
	static CountDownLatch start = new CountDownLatch(1);
	
	// main线程将会等待所有ConnectionRunner结束后才能继续执行
	static CountDownLatch end;
	
	public static void main(String[] args) {
		// 线程数量,可以修改线程数量进行观察
		int threadCount = 10;
		end = new CountDownLatch(threadCount);
		int count = 20;
		AtomicInteger got = new AtomicInteger();
		AtomicInteger notgot = new AtomicInteger();
		for (int i = 0; i < threadCount; i++) {
			Thread thread = new Thread(new ConnectionRunner(count, got, notgot),
					"ConnectionRunnerThread");
			thread.start();
		}
		start.countDown();
		System.out.println("total invoke:" + (threadCount * count));
		System.out.println("got connection:" + got);
		System.out.println("not got connection" + notgot);
	}
	
	static class ConnectionRunner implements Runnable {
		int count;
		AtomicInteger got;
		AtomicInteger notGot;
		
		
		public ConnectionRunner(int count, AtomicInteger got, AtomicInteger notGot) {
			this.count = count;
			this.got = got;
			this.notGot = notGot;
		}

		@Override
		public void run() {
			try {
				start.await();
			} catch (Exception e) {
			}
			while (count > 0) {
				try {
					// 从线程池中获取连接,如果1000毫秒内无法获取到,将会返回null
					// 分别统计连接获取的数量got和未获取到的数量notgot
					Connection connection = pool.fetchConnection(1000);
					if (connection != null) {
						try {
							connection.createStatement();
							connection.commit();
						} catch (Exception e) {
						} finally {
							pool.relaseConnection(connection);
							got.incrementAndGet();
						}
					} else {
						notGot.incrementAndGet();
					}
				} catch (Exception e) {
				} finally {
					count--;
				}
			}
			end.countDown();
		}
		
		
	}
}
