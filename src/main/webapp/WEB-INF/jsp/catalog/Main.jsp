<%--

       Copyright 2010-2026 the original author or authors.

       Licensed under the Apache License, Version 2.0 (the "License");
       you may not use this file except in compliance with the License.
       You may obtain a copy of the License at

          https://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing, software
       distributed under the License is distributed on an "AS IS" BASIS,
       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
       See the License for the specific language governing permissions and
       limitations under the License.

--%>
<%@ include file="../common/IncludeTop.jsp"%>

<div id="Welcome">
<div id="WelcomeContent"><c:if test="${actionBean.pageView.authenticated}">
    Welcome ${actionBean.pageView.authenticatedUser.displayName}!
</c:if></div>
</div>

<div id="Main">
<div id="Sidebar">
<div id="SidebarContent"><c:forEach var="category"
	items="${catalogNavigation.sidebarCategories}">
	<stripes:link
		beanclass="org.mybatis.jpetstore.catalog.web.CatalogActionBean"
		event="viewCategory">
		<stripes:param name="categoryId" value="${category.categoryId}" />
		<img src="${category.iconImagePath}" alt="${category.name}" />
	</stripes:link> <br />
	${category.sidebarDescription} <br />
</c:forEach></div>
</div>

<div id="MainImage">
<div id="MainImageContent">
  <map name="estoremap">
	<c:forEach var="area" items="${catalogNavigation.mainImageMapAreas}">
		<area alt="${area.name}" coords="${area.coordinates}"
			href="Catalog.action?viewCategory=&categoryId=${area.categoryId}"
			shape="RECT" />
	</c:forEach>
  </map>
  <img height="355" src="../images/splash.gif" align="middle"
	usemap="#estoremap" width="350" /></div>
</div>

<div id="Separator">&nbsp;</div>
</div>

<%@ include file="../common/IncludeBottom.jsp"%>
