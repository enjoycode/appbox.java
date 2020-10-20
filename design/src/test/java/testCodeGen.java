import appbox.design.services.CodeGenService;
import appbox.model.EntityModel;
import appbox.model.entity.DataFieldModel;
import org.junit.jupiter.api.Test;

public class testCodeGen {

    @Test
    public void test(){
        EntityModel model =new EntityModel();
        model.set_designMode(true);
        model.set_name("user");
        DataFieldModel filed1 =new DataFieldModel(model,"id", DataFieldModel.DataFieldType.Int,false,false);
        DataFieldModel filed2 =new DataFieldModel(model,"name", DataFieldModel.DataFieldType.String,false,false);
        DataFieldModel filed3 =new DataFieldModel(model,"age", DataFieldModel.DataFieldType.Int,false,false);
        DataFieldModel filed4 =new DataFieldModel(model,"sex", DataFieldModel.DataFieldType.Byte,false,false);
        try {
            model.addMember(filed1,false);
            model.addMember(filed2,false);
            model.addMember(filed3,false);
            model.addMember(filed4,false);
            String entity= CodeGenService.GenEntityDummyCode(model,"test",null);
            System.out.println(entity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
