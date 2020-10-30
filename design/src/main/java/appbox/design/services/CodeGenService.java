package appbox.design.services;

import appbox.design.tree.DesignTree;
import appbox.model.EntityModel;
import appbox.model.entity.DataFieldModel;
import appbox.model.entity.EntityMemberModel;
import appbox.utils.StringUtil;

import java.util.List;

/**
 * 用于生成各模型的虚拟代码
 */
public class CodeGenService {

    /**
     * 根据实体模型生成虚拟代码
     * @param model
     * @param appName
     * @param designTree
     * @return
     */
    public static String genEntityDummyCode(EntityModel model, String appName, DesignTree designTree){
        StringBuffer sb=new StringBuffer();
        sb.append("package com."+appName+".entities;\n\n");
        //append import
        sb.append("public class "+StringUtil.firstUpperCase(model.name())+" {\n\n");
        //append constructor
        sb.append("\tpublic "+StringUtil.firstUpperCase(model.name())+"() {}\n\n");
        //append propertites
        List<EntityMemberModel> memberList = model.get_members();
        for(EntityMemberModel memberModel:memberList){
            DataFieldModel modelField=(DataFieldModel)memberModel;
            sb.append("\tprivate "+modelField.get_dataType().name()+" "+memberModel.name()+";\n");
        }
        sb.append("\n");
        //append get set
        for(EntityMemberModel memberModel:memberList){
            DataFieldModel modelField=(DataFieldModel)memberModel;
            sb.append("\tpublic "+modelField.get_dataType().name()+" get"+ StringUtil.firstUpperCase(memberModel.name())+"(){return "+memberModel.name()+";}\n");
            sb.append("\tpublic void set"+StringUtil.firstUpperCase(memberModel.name())+"("+modelField.get_dataType().name()+" "+memberModel.name()+"){this."+memberModel.name()+"="+memberModel.name()+";}\n");
        }
        sb.append("}");
        return sb.toString();
    }

}
