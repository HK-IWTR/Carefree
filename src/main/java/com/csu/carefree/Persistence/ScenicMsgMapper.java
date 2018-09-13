package com.csu.carefree.Persistence;

import com.csu.carefree.Model.TraverMsg.CityMsg;
import com.csu.carefree.Model.TraverMsg.ScenicMsg;

import java.util.List;

public interface ScenicMsgMapper {

    //通过城市ID获取城市信息
    CityMsg getCityMsgById(String cityId);

    //通过景点ID获取景点信息
    ScenicMsg getScenicMsgById(String scenicId);

    //通过景点名称获取景点信息
    List<ScenicMsg> getScenicMsgListByName(String name);
}