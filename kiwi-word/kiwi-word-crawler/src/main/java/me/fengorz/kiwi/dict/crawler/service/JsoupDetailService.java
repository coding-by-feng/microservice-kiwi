/*
 *
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.dict.crawler.service;

/**
 * 假如会有n个词典数据源可供爬虫抓取，但是每个数据源的抓取规则都不一样，需要抽象的处理一些逻辑如下： - 前提： -- 最后入库的数据模型一定是统一的，哪怕数据不统一，但是模型要柔和成一个。 -- 使用哪些设计模式来重构？ ---
 * 装饰者模式 - 处理过程： -- 切换不同的爬虫url。 -- 每一个读取html标签元素的动作要视为一个不可分割的原子性操作，抓取逻辑是由n个原子操作可自由组合形成的，要采用插槽式设计理念，可插拔式爬虫抓取。 --
 * 抽取数据的层级差异化： --- 切换到Cambridge的英英模式，就不会抓取释义和例句的中文翻译。（考虑异步到有道拿释义和例句的翻译） ---
 * 切换到科林词典的英汉模式，抓取词性、释义和词性标签的html模板完全不一样要重新写抓取规则 --- 有些例句有自动附带翻译和发音，需要做差异化抽取。 --- ...
 */
public interface JsoupDetailService {}
