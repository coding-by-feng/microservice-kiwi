package me.fengorz.kiwi.common.sdk.util.spring;

import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** spring工具类 方便在非spring管理环境中获取bean */
public class SpringUtils {
  private static ApplicationContext applicationContext = null;

  public static void init(ApplicationContext applicationContext) {
    if (SpringUtils.applicationContext != null) {
      return;
    }
    SpringUtils.applicationContext = applicationContext;
  }

  public static ApplicationContext getApplicationContext() {
    return applicationContext;
  }

  /***
   * 根据一个bean的id获取配置文件中相应的bean
   *
   * @param name
   * @return
   * @throws BeansException
   */
  @SuppressWarnings("unchecked")
  public static <T> T getBean(String name) throws BeansException {
    return (T) applicationContext.getBean(name);
  }

  /***
   * 类似于getBean(String name)只是在参数中提供了需要返回到的类型。
   *
   * @param name
   * @param requiredType
   * @return
   * @throws BeansException
   */
  public static <T> T getBean(String name, Class<T> requiredType) throws BeansException {
    return applicationContext.getBean(name, requiredType);
  }

  public static <T> T getBean(Class<T> requiredType) throws BeansException {
    return applicationContext.getBean(requiredType);
  }

  public static <T> Map<String, T> getBeansMap(Class<T> requiredType) {
    return applicationContext.getBeansOfType(requiredType);
  }

  public static <T> List<T> getBeansList(Class<T> requiredType) {
    Map<String, T> beansOfType = applicationContext.getBeansOfType(requiredType);
    if (KiwiCollectionUtils.isEmpty(beansOfType)) {
      return null;
    }
    List<T> list = new ArrayList<>();
    return new ArrayList<>(beansOfType.values());
  }

  /**
   * 如果BeanFactory包含一个与所给名称匹配的bean定义，则返回true
   *
   * @param name
   * @return boolean
   */
  public static boolean containsBean(String name) {
    return applicationContext.containsBean(name);
  }

  /**
   * 判断以给定名字注册的bean定义是一个singleton还是一个prototype。
   * 如果与给定名字相应的bean定义没有被找到，将会抛出一个异常（NoSuchBeanDefinitionException）
   *
   * @param name
   * @return boolean
   * @throws NoSuchBeanDefinitionException
   */
  public static boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
    return applicationContext.isSingleton(name);
  }

  /**
   * @param name
   * @return Class 注册对象的类型
   * @throws NoSuchBeanDefinitionException
   */
  public static Class<?> getType(String name) throws NoSuchBeanDefinitionException {
    return applicationContext.getType(name);
  }

  /**
   * 如果给定的bean名字在bean定义中有别名，则返回这些别名
   *
   * @param name
   * @return
   * @throws NoSuchBeanDefinitionException
   */
  public static String[] getAliases(String name) throws NoSuchBeanDefinitionException {
    return applicationContext.getAliases(name);
  }

  public static String[] getBeanNames(Class<?> type) {
    return applicationContext.getBeanNamesForType(type);
  }

  /**
   * 获取aop代理对象
   *
   * @param invoker
   * @return
   */
  @SuppressWarnings("unchecked")
  public static <T> T getAopProxy(T invoker) {
    return (T) AopContext.currentProxy();
  }
}
