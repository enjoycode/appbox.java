import appbox.design.services.CodeGenService;
import appbox.model.EntityModel;
import appbox.model.entity.DataFieldModel;
import org.junit.jupiter.api.Test;

public class TestCodeGen {

    @Test
    public void testEntityModel() {
        var model  = new EntityModel(1L, "User");
        var filed2 = new DataFieldModel(model, "Name", DataFieldModel.DataFieldType.String, false);
        var filed3 = new DataFieldModel(model, "Age", DataFieldModel.DataFieldType.Int, true);
        var filed4 = new DataFieldModel(model, "Sex", DataFieldModel.DataFieldType.Byte, false);
        model.addMember(filed2, false);
        model.addMember(filed3, false);
        model.addMember(filed4, false);
        String entity = CodeGenService.genEntityDummyCode(model, "test", null);
        System.out.println(entity);
    }
}
