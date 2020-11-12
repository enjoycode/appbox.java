import appbox.utils.DynamicClassFactory;
import appbox.utils.MyClassLoader;
import appbox.utils.JavaCompilerUtil;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;


public class TestClassLoader {
    //测试compilerUtil
    @Test
    public void testCompilerUtil(){
        String javaPath="/home/panmingjie/TEMP/Test.java";
        String outPutClassPath ="/home/panmingjie/TEMP/";
        List<String> list =new ArrayList<>();
        list.add(javaPath);
        boolean result =JavaCompilerUtil.compilerJavaFile(list,outPutClassPath);
        if(result){
            System.out.println("编译成功。");
        }
    }
    //自定义classloader加载类
    @Test
    public void classLoader() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        ClassLoader loader = new MyClassLoader();
        Class<?> clazz = loader.loadClass("com.frank.test.Test");
        Object obj    = clazz.newInstance();
        Method method = clazz.getMethod("say", new Class<?>[] {});
        Object value = method.invoke(obj, new Object[] {});
    }

   //测试ASM动态生成字节码文件
   @Test
   public void testASMGenerateClass() throws IOException {
       //生成一个类只需要ClassWriter组件即可
       ClassWriter cw = new ClassWriter(0);
       //通过visit方法确定类的头部信息
       cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC+Opcodes.ACC_ABSTRACT+Opcodes.ACC_INTERFACE,
               "com/asm3/Comparable", null, "java/lang/Object", new String[]{"com/asm3/Mesurable"});
       //定义类的属性
       cw.visitField(Opcodes.ACC_PUBLIC+Opcodes.ACC_FINAL+Opcodes.ACC_STATIC,
               "LESS", "I", null, new Integer(-1)).visitEnd();
       cw.visitField(Opcodes.ACC_PUBLIC+Opcodes.ACC_FINAL+Opcodes.ACC_STATIC,
               "EQUAL", "I", null, new Integer(0)).visitEnd();
       cw.visitField(Opcodes.ACC_PUBLIC+Opcodes.ACC_FINAL+Opcodes.ACC_STATIC,
               "GREATER", "I", null, new Integer(1)).visitEnd();
       //定义类的方法
       cw.visitMethod(Opcodes.ACC_PUBLIC+Opcodes.ACC_ABSTRACT, "compareTo",
               "(Ljava/lang/Object;)I", null, null).visitEnd();
       cw.visitEnd(); //使cw类已经完成
       //将cw转换成字节数组写到文件里面去
       byte[]           data = cw.toByteArray();
       File             file = new File("/home/panmingjie/TEMP/Comparable.class");
       FileOutputStream fout = new FileOutputStream(file);
       fout.write(data);
       fout.close();
   }

   @Test
   public void testASMCompile() throws IOException {
       try {
           InputStream stream =new FileInputStream(new File("/home/panmingjie/TEMP/Comparable.class"));
           ClassReader cr = new ClassReader(stream);
           ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
           cr.accept(cw, ClassReader.SKIP_DEBUG);
           //TODO 动态修改class字节码信息
           byte[] data = cw.toByteArray();
           File file = new File("/home/panmingjie/TEMP/Comparable1.class");
           FileOutputStream fout = new FileOutputStream(file);
           fout.write(data);
           fout.close();
           System.out.println("success!");
       } catch (FileNotFoundException e) {
           e.printStackTrace();
       } catch (IOException e) {
           e.printStackTrace();
       }
   }
    private static final String CLASS_ROOT_PATH="/home/rick/out/";
   @Test
   public void testCreateClass() throws Exception {
        String className="MyTest";
        String fullName="com/model/"+className;
        byte[] code=DynamicClassFactory.getClassByte(className);
        code=DynamicClassFactory.addProperty(className,"id",Integer.class);

        MyClassLoader classLoader=new MyClassLoader();
        Class      clz =classLoader.defineClassPublic(fullName.replace("/","."), code, 0, code.length);
        Constructor con =clz.getConstructor(long.class);
        Object     obj = con.newInstance(10l);
        Method method = clz.getMethod("setId", Integer.class);
        method.invoke(obj, 1);

        Method method1 = clz.getMethod("getId", new Class<?>[] {});
        Object value1 = method1.invoke(obj, new Object[] {});
        System.out.println(value1.toString());
   }
}
