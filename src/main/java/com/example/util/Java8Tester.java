package com.example.util;


public class Java8Tester {
	/**
	 * // 1. 不需要参数,返回值为 5  
() -> 5  
  
// 2. 接收一个参数(数字类型),返回其2倍的值  
x -> 2 * x  
  
// 3. 接受2个参数(数字),并返回他们的差值  
(x, y) -> x – y  
  
// 4. 接收2个int型整数,返回他们的和  
(int x, int y) -> x + y  
  
// 5. 接受一个 string 对象,并在控制台打印,不返回任何值(看起来像是返回void)  
(String s) -> System.out.print(s)  
	 */
	public void a(int x) {
		
	}
	public static void main(String[] args) {
		//lambda表达式，示例。 函数可以作为参数。（）中需要填写run方法的参数。花括号是方法体。
		new Thread(()->{
			
			System.out.println("23");
			
		}).start();
		/*
		 * 	原代码，java中用@FunctionalInterface注解标注的类都可以使用lambda表达式
		 */
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				System.out.println("23");
			}
		}).start();
	}
}
