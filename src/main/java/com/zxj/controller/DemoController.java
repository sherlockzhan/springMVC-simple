package com.zxj.controller;

import com.annotation.SXAutowired;
import com.annotation.SXController;
import com.annotation.SXRequestMapping;
import com.zxj.service.DemoService;

@SXController
@SXRequestMapping("/demo")
public class DemoController {

	@SXAutowired
	private DemoService demoService;
	
	@SXRequestMapping("/getName")
	public void getName() {
		System.out.println("====================反射成功================");
	}
}
