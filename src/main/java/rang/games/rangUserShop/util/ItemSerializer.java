package rang.games.rangUserShop.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ItemSerializer {

    private static final Logger log = Logger.getLogger("Minecraft");

    /**
     * ItemStack 객체를 Base64 문자열로 변환합니다.
     * @param itemStack 직렬화할 ItemStack
     * @return Base64로 인코딩된 문자열
     * @throws IOException 직렬화 과정에서 오류 발생 시
     */
    public static String itemStackToBase64(ItemStack itemStack) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeObject(itemStack);
            return Base64Coder.encodeLines(outputStream.toByteArray());
        }
    }

    /**
     * Base64 문자열을 ItemStack 객체로 변환합니다.
     * @param base64 역직렬화할 Base64 문자열
     * @return 변환된 ItemStack 객체
     * @throws IOException 역직렬화 과정에서 I/O 오류 발생 시
     * @throws ClassNotFoundException 클래스를 찾을 수 없을 때
     */
    public static ItemStack base64ToItemStack(String base64) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(base64));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {

            return (ItemStack) dataInput.readObject();
        }
    }
}