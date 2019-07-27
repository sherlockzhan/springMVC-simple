package com.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.SynchronousQueue;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.plaf.synth.SynthOptionPaneUI;

import com.annotation.SXAutowired;
import com.annotation.SXController;
import com.annotation.SXRequestMapping;
import com.annotation.SXService;
import com.zxj.controller.DemoController;

public class SXDispatchServlet extends HttpServlet {

	private static final long serialVersionUID = -5205842397769611649L;

	private Properties p = new Properties();
	private List<String> classNames = new ArrayList<>();
	private Map<String, Object> ioc = new HashMap<>();
	private Map<String, Object> handlerMapping = new HashMap<>();

	@Override
	public void init(ServletConfig config) throws ServletException {

		// 1.加载配置文件
		doLoadConfig(config.getInitParameter("contextConfigLocation"));
		// 2.扫描所有类
		doScanner(p.getProperty("scanPackage"));
		// 3.实例化对象
		doInstance();
		// 4.依赖注入
		doAutoWired();
		// 5.初始化handlerMapping
		initHandlerMapping();

		System.out.println("---------项目启动-----------");
	}

	private void initHandlerMapping() {
		if (ioc.isEmpty()) {
			return;
		}

		for (Map.Entry<String, Object> entry : ioc.entrySet()) {
			Class<?> clazz = entry.getValue().getClass();

			if (!clazz.isAnnotationPresent(SXController.class)) {
				continue;
			}

			String baseUrl = "";
			if (clazz.isAnnotationPresent(SXRequestMapping.class)) {
				SXRequestMapping sxRequestMapping = clazz.getAnnotation(SXRequestMapping.class);
				baseUrl = sxRequestMapping.value();
			}

			Method[] methods = clazz.getMethods();
			for (Method method : methods) {
				if (!method.isAnnotationPresent(SXRequestMapping.class)) {
					continue;
				}
				SXRequestMapping requestMapping = method.getAnnotation(SXRequestMapping.class);
				baseUrl += requestMapping.value();
				handlerMapping.put(baseUrl, method);
			}
		}
	}

	private void doAutoWired() {
		if (ioc.isEmpty()) {
			return;
		}

		for (Map.Entry<String, Object> entry : ioc.entrySet()) {
			Field[] fields = entry.getValue().getClass().getDeclaredFields();

			for (Field field : fields) {
				if (!field.isAnnotationPresent(SXAutowired.class)) {
					continue;
				}

				SXAutowired sxAutowired = field.getAnnotation(SXAutowired.class);
				String beanName = sxAutowired.value().trim();

				if ("".equals(beanName)) {
					beanName = field.getType().getName();
				}

				field.setAccessible(true);
				try {
					field.set(entry.getValue(), ioc.get(beanName));
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					continue;
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					continue;
				}

			}
		}

	}

	private void doInstance() {
		if (classNames.isEmpty()) {
			return;
		}
		try {
			for (String className : classNames) {
				Class<?> clazz = Class.forName(className);

				if (clazz.isAnnotationPresent(SXController.class)) {
					SXRequestMapping sxRequestMapping = clazz.getAnnotation(SXRequestMapping.class);
					
					//String beanName = lowerFirstCase(clazz.getSimpleName());
					String beanName = sxRequestMapping.value();
					ioc.put(beanName, clazz.newInstance());
					System.out.println(beanName);
				} else if (clazz.isAnnotationPresent(SXService.class)) {

					SXService sxService = clazz.getAnnotation(SXService.class);
					String beanName = sxService.value();
					if ("".equals(beanName.trim())) {
						beanName = lowerFirstCase(clazz.getSimpleName());
					}
					Object instance = clazz.newInstance();
					ioc.put(beanName, instance);

					Class<?>[] interfaces = clazz.getInterfaces();
					for (Class<?> i : interfaces) {
						ioc.put(i.getName(), instance);
					}
				} else {
					continue;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 首字母转化为小写
	private String lowerFirstCase(String simpleName) {
		char[] sn = simpleName.toCharArray();
		sn[0] += 32;
		return String.valueOf(sn);
	}

	private void doScanner(String packageName) {
		URL url = this.getClass().getResource("/" + packageName.replaceAll("\\.", "/"));
		File classDir = new File(url.getFile());
		for (File file : classDir.listFiles()) {
			if (file.isDirectory()) {
				doScanner(packageName + "." + file.getName());
			} else {
				String className = (packageName + "." + file.getName().replace(".class", ""));
				classNames.add(className);
			}
		}
	}

	private void doLoadConfig(String location) {
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(location);
		try {
			p.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (ioc.isEmpty()) {
			return;
		}

		String contextPath = req.getContextPath();
		String uri = req.getRequestURI();
		String path = uri.replace(contextPath, "").replaceAll("/+", "/");

		if (!handlerMapping.containsKey(path)) {
			resp.getWriter().write("404 not found");
			return;
		}

		Method method = (Method) handlerMapping.get(path);

		Object instance = ioc.get("/"+path.split("/")[1]);
		//Object instance = ioc.get("demoController");

		try {
			method.invoke(instance);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

	}
}
