package com.zxj.controller;

import com.annotation.SXController;
import com.annotation.SXRequestMapping;

@SXController
@SXRequestMapping("/test2")
public class Test2Controller {

	@SXRequestMapping("/getName")
	public void getName() {
		System.out.println("=======反射成功test2==========");
	}
}
