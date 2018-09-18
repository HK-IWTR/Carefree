package com.csu.carefree.Controller;


import com.csu.carefree.Util.PageInfo;
import com.csu.carefree.Model.TraverMsg.ScenicMsg;
import com.csu.carefree.Model.TraverMsg.TraverMsg;
import com.csu.carefree.Service.CatalogService;
import com.csu.carefree.Model.ProductDT.*;
import com.csu.carefree.Model.TraverAsk.TraverNote;
import com.csu.carefree.Util.CatalogUtils;
import com.csu.carefree.Util.LocationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class CatalogController {
    /**
     * 实现产品推荐展示模块的业务逻辑
     * 实现与酒店有关的业务逻辑
     * 酒店信息展示，热门酒店推荐
     */


    private static final int PRODUCTPAGESIZE = 8;
    @Autowired
    private CatalogService catalogService;

    private CatalogUtils catalogUtils = new CatalogUtils();

    @GetMapping("ProductDT/viewHotel")
    public String viewHotelMsgList(Model model) {
        model.addAttribute("HotelList");
        return "Hotel";
    }


    //请求主界面
    @GetMapping("/")
    public String viewIndex(HttpSession session, Model model) {

        //首先调用百度地图api获得当前城市信息,如果获取失败,则设为长沙
        String location = null;
        try {
            location = LocationUtil.getCityLocation();
        } catch (Exception e) {
            location = "长沙";
        }
        if (location == null) {
            location = "长沙";
        }
        if (session.getAttribute("location") == null) {
            //第一次访问,首先设置session存入当前位置
            //设置会话中的location,全局使用
            session.setAttribute("location", location);
        }
        session.setAttribute("hotProductcheckedDaysAll", true);
        session.setAttribute("hotProductcheckedStoreAll", true);
        session.setAttribute("hotProductcheckedTypeAll", true);
        //行程信息存在session当中
        TraverMsg traverMsg = (TraverMsg) session.getAttribute("traverMsg");
        //还没有填写表单信息
        if (traverMsg == null) {
            traverMsg = new TraverMsg();
            //设置默认起始地为session中的当前位置
            traverMsg.setStart_city((String) session.getAttribute("location"));
            //行程信息存到session中
            session.setAttribute("traverMsg", traverMsg);
        }

        /*****************************热门产品推荐*********************************/
        List<FullProductInfo> hotProductList = catalogService.getHotProductList(session);
        session.setAttribute("product1", hotProductList.get(0));
        session.setAttribute("product2", hotProductList.get(1));

        FullProductInfo product1 = session.getAttribute("product1")
                == null ? new FullProductInfo() : (FullProductInfo) session.getAttribute("product1");
        String product1Price = (catalogService.getDepartCityPrice(product1.getId(), (String) session.getAttribute("location"))).getProduct_price();
        product1.setPrice(product1Price);//设置价格
        FullProductInfo product2;
        if (session.getAttribute("product2") == null)
            product2 = new FullProductInfo();
        else
            product2 = (FullProductInfo) session.getAttribute("product2");
        String product2Price = (catalogService.getDepartCityPrice(product2.getId(), (String) session.getAttribute("location"))).getProduct_price();
        product2.setPrice(product2Price);

        /***************************热门游记推荐*********************************/
//        List<TraverNote> hotTraverNoteList = catalogService.getHotTraverNoteList();
//        System.out.println("热门游记个数："+ hotProductList.size());
//        session.setAttribute("hotTraverNoteList", hotTraverNoteList);

        /****************************热门酒店推荐********************************/
        //热门酒店推荐这个城市排名最高个酒店
        //1.价格最便宜的酒店
        List<HotelMsg> hotHotelList_01 = catalogService.getHotHotelListByCityName((String) session.getAttribute("location"), 1).subList(0, 4);
        //2.评分最高的酒店
        List<HotelMsg> hotHotelList_02 = catalogService.getHotHotelListByCityName((String) session.getAttribute("location"), 2).subList(0, 4);
        //存储数据到前端页面
        //1. 酒店信息
        model.addAttribute("hotHotelList_01", hotHotelList_01);
        model.addAttribute("hotHotelList_02", hotHotelList_02);
        //2. 热门产品信息
        model.addAttribute("product1", product1);
        model.addAttribute("product2", product2);
        return "index";
    }


    //不填写表单直接跳转到目的地界面
    @GetMapping("/Catalog/Mdd")
    public String ViewMdd(HttpSession session) {
        /*****************************景点推荐*********************************/
        List<ScenicMsg> recommendScenicList = catalogService.getRecommendScenicList(session);
        System.out.println("-----------------------景点的个数" + recommendScenicList.size());
        session.setAttribute("recommendScenicList", recommendScenicList);


        /*****************************酒店推荐*********************************/
        List<HotelMsg> recommendHotelList = catalogService.getRecommendHotelList(session);
        System.out.println("-----------------------宾馆的个数" + recommendHotelList.size());
        session.setAttribute("recommendHotelList", recommendHotelList);

        /*****************************攻略推荐*********************************/
        List<StrategyMsg> recommendStrategyList = catalogService.getRecommendStrategyList(session);
        System.out.println("-----------------------攻略的个数" + recommendStrategyList.size());
        session.setAttribute("recommendStrategyList", recommendStrategyList);

        /*****************************游记推荐*********************************/
        List<TraverNote> recommendTraverNoteList = catalogService.getRecommendTraverNoteList(session);
        System.out.println("-----------------------游记的个数" + recommendTraverNoteList.size());
        session.setAttribute("recommendTraverNoteList", recommendTraverNoteList);
        return "ProductDT/Mdd";
    }

    //填写表单请求跳转目的地界面
    @PostMapping("/Catalog/Mdd")
    public String ViewMdd(@RequestParam("startCity") String startCity, @RequestParam("destination") String destination,
                          @RequestParam("adultNum") String adultNum, @RequestParam("childrenNum") String childrenNum,
                          @RequestParam("travelDays") String travelDays, HttpSession session, Model model) {
        //行程数据
        TraverMsg traverMsg;
        if (session.getAttribute("traverMsg") == null)
            traverMsg = new TraverMsg();
        else
            traverMsg = (TraverMsg) session.getAttribute("traverMsg");
        //保证字段不为空
        traverMsg.setTraverdays(travelDays == null ? "" : travelDays);
        traverMsg.setAdult_num(adultNum == null ? "" : adultNum);
        traverMsg.setChild_num(childrenNum == null ? "" : childrenNum);
        traverMsg.setStart_city(startCity == null ? "" : startCity);
        traverMsg.setEnd_city(destination == null ? "" : destination);

        /*****************************景点推荐*********************************/
        List<ScenicMsg> recommendScenicList = catalogService.getRecommendScenicList(session);
        System.out.println("-----------------------景点的个数" + recommendScenicList.size());
        session.setAttribute("recommendScenicList", recommendScenicList);


        /*****************************酒店推荐*********************************/
        List<HotelMsg> recommendHotelList = catalogService.getRecommendHotelList(session);
        System.out.println("-----------------------宾馆的个数" + recommendHotelList.size());
        session.setAttribute("recommendHotelList", recommendHotelList);

        /*****************************攻略推荐*********************************/
        List<StrategyMsg> recommendStrategyList = catalogService.getRecommendStrategyList(session);
        System.out.println("-----------------------攻略的个数" + recommendStrategyList.size());
        session.setAttribute("recommendStrategyList", recommendStrategyList);

        /*****************************游记推荐*********************************/
        List<TraverNote> recommendTraverNoteList = catalogService.getRecommendTraverNoteList(session);
        System.out.println("-----------------------游记的个数" + recommendTraverNoteList.size());
        session.setAttribute("recommendTraverNoteList", recommendTraverNoteList);
        return "ProductDT/Mdd";
    }


    //进入热门产品的界面控制器url
    @RequestMapping("/Catalog/HotProductList")
    public String HotProductList(Model model,
                                 HttpSession session,
                                 @RequestParam(defaultValue = "1") Integer pageNum) {

        String destination = (String) session.getAttribute("location");
        //首先获得数据
        List<ProductMsg> productMsgList = catalogService.getProductListByCityName(destination);
        Map<String, List<ProductMsg>> map = new HashMap<>();
        for (int i = 0; i < productMsgList.size() / PRODUCTPAGESIZE; ++i) {
            map.put(String.valueOf(++i), productMsgList.subList(i, i + PRODUCTPAGESIZE));
        }
        map.get(pageNum);

        System.out.println(pageNum + "    " + PRODUCTPAGESIZE);
        List<ProductMsg> CurrentPageList = productMsgList.subList(pageNum * PRODUCTPAGESIZE, pageNum * PRODUCTPAGESIZE + PRODUCTPAGESIZE);
        PageInfo<ProductMsg> pageInfo = new PageInfo<>();
        pageInfo.setTotal(productMsgList.size());
        pageInfo.setPageData(CurrentPageList);
        //设置当前界面
        pageInfo.setCurrentPage(pageNum);
        if (pageInfo.getCurrentPage() == 0)
            pageInfo.setFirstPage(true);
        else
            pageInfo.setFirstPage(false);
        if (pageInfo.getCurrentPage() == pageInfo.getMaxPage())
            pageInfo.setLastPage(true);
        else
            pageInfo.setLastPage(false);
        model.addAttribute("pageInfo", pageInfo);
        //获得一页显示的条数
        model.addAttribute("pageSize", pageInfo.getPageSize());
        model.addAttribute("pageNum", pageInfo.getCurrentPage());
        //是否是第一页
        model.addAttribute("isFirstPage", pageInfo.isFirstPage());
        //获得总页数
        model.addAttribute("totalPages", pageInfo.getMaxPage());
        //是否是最后一页
        model.addAttribute("isLastPage", pageInfo.isLastPage());
        System.out.println("找到符合条件的产品" + productMsgList.size() + "条");
        model.addAttribute("productMsgList", productMsgList);
        return "ProductDT/Product";
    }


    /////////////////////////////////////////////////
    //进入热门产品的界面控制器url
    @PostMapping("/Catalog/HotProductListByConditions")
    public String HotProductListByConditions(HttpServletRequest httpServletRequest, HttpSession session, Model model) {
        String[] checkedDaysValues = httpServletRequest.getParameterValues("days");
        String[] checkedStoreValues = httpServletRequest.getParameterValues("store");
        String[] checkedTypeValues = httpServletRequest.getParameterValues("type");
        System.out.println(checkedDaysValues[0]);
        System.out.println(checkedStoreValues[0]);
        System.out.println(checkedTypeValues[0]);
        String traverDays = checkedDaysValues[0];
        String supplierId = checkedStoreValues[0];
        String productType = checkedTypeValues[0];
        List<ProductMsg> productMsgList = new ArrayList<ProductMsg>();
        if (checkedDaysValues[0].equals("0") && checkedStoreValues[0].equals("0") && checkedTypeValues[0].equals("0")) {
            productMsgList = catalogService.getProductList();
        }
        if (!checkedDaysValues[0].equals("0") && checkedStoreValues[0].equals("0") && checkedTypeValues[0].equals("0")) {
            productMsgList = catalogService.getProductListByTraverdays(traverDays);
        }
        if (checkedDaysValues[0].equals("0") && !checkedStoreValues[0].equals("0") && checkedTypeValues[0].equals("0")) {
            productMsgList = catalogService.getProductListBySupplierId(supplierId);
        }
        if (checkedDaysValues[0].equals("0") && checkedStoreValues[0].equals("0") && !checkedTypeValues[0].equals("0")) {
            productMsgList = catalogService.getProductListByProductType(productType);
        }
        if (!checkedDaysValues[0].equals("0") && !checkedStoreValues[0].equals("0") && checkedTypeValues[0].equals("0")) {
            productMsgList = catalogService.getProductListByDaysAndStore(traverDays, supplierId);
        }
        if (!checkedDaysValues[0].equals("0") && checkedStoreValues[0].equals("0") && !checkedTypeValues[0].equals("0")) {
            productMsgList = catalogService.getProductListByDaysAndType(traverDays, productType);
        }
        if (checkedDaysValues[0].equals("0") && !checkedStoreValues[0].equals("0") && !checkedTypeValues[0].equals("0")) {
            productMsgList = catalogService.getProductListByTypeAndStore(productType, supplierId);
        }
        if (!checkedDaysValues[0].equals("0") && !checkedStoreValues[0].equals("0") && !checkedTypeValues[0].equals("0")) {
            productMsgList = catalogService.getProductListByThree(traverDays, productType, supplierId);
        }
        System.out.println("找到符合条件的产品" + productMsgList.size() + "条");
        String destination = session.getAttribute("location").toString();
        catalogUtils.setDepartCityPrice(catalogService, destination, productMsgList);
        model.addAttribute("productMsgList", productMsgList);
        return "ProductDT/Product";
    }


    //进入酒店页面的界面控制器
    @GetMapping("/Catalog/HotHotelList")
    public String HotHotelList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            HttpSession session,
            Model model) {
        String destination = (String) session.getAttribute("location");


        //获取当前用户位置,推荐酒店
        if (destination != null) {
            List<HotelMsg> hotelMsgList = catalogService.getHotelListByDestination(destination + "市");
            System.out.println(hotelMsgList.size());
            model.addAttribute("hotelMsgList", hotelMsgList);
            model.addAttribute("destination", destination);
        }

        PageInfo<HotelMsg> hotelMsgPageInfo = new PageInfo<>();
        List<HotelMsg> hotelMsgList = catalogService.getHotelMsgList();
        List<HotelMsg> currentPageList = hotelMsgList.subList(pageNum * PRODUCTPAGESIZE, pageNum * PRODUCTPAGESIZE + PRODUCTPAGESIZE);
        hotelMsgPageInfo.setPageData(currentPageList);
        hotelMsgPageInfo.setTotal(hotelMsgList.size());

        hotelMsgPageInfo.setCurrentPage(pageNum);
        if (hotelMsgPageInfo.getCurrentPage() == 0)
            hotelMsgPageInfo.setFirstPage(true);
        else
            hotelMsgPageInfo.setFirstPage(false);
        if (hotelMsgPageInfo.getCurrentPage() == hotelMsgPageInfo.getMaxPage())
            hotelMsgPageInfo.setLastPage(true);
        else
            hotelMsgPageInfo.setLastPage(false);
        model.addAttribute("hotelPageInfo", hotelMsgPageInfo);

        return "ProductDT/Hotel";
    }

    @PostMapping("/Catalog/HotHotelListByConditions")
    public String HotHotelListByConditions(HttpServletRequest httpServletRequest, HttpSession session, Model model) {
        String[] checkedStoreValues = httpServletRequest.getParameterValues("store");
        String[] checkedPriceValues = httpServletRequest.getParameterValues("price");

        System.out.println(checkedStoreValues[0]);
        System.out.println(checkedPriceValues[0]);

        String supplierId = checkedStoreValues[0];
        String price = checkedPriceValues[0];

        String destination = session.getAttribute("location").toString();

        List<HotelMsg> hotelMsgList = new ArrayList<HotelMsg>();

        if (supplierId.equals("0") && price.equals("0")) {
            hotelMsgList = catalogService.getHotelListByDestination(destination + "市");
        }
        if (!supplierId.equals("0") && price.equals("0")) {
            hotelMsgList = catalogService.getHotelListByDestinationAndStore(destination + "市", supplierId);
        }
        if (supplierId.equals("0") && !price.equals("0")) {
            hotelMsgList = catalogService.getHotelListByDestination(destination + "市");
            catalogUtils.setHotelListByPrices(hotelMsgList, price);
        }
        if (!supplierId.equals("0") && !price.equals("0")) {
            hotelMsgList = catalogService.getHotelListByDestinationAndStore(destination + "市", supplierId);
            catalogUtils.setHotelListByPrices(hotelMsgList, price);
        }
        model.addAttribute("hotelMsgList", hotelMsgList);
        return "ProductDT/Hotel";
    }
    /****************************攻略推荐模块**********************************/
}
