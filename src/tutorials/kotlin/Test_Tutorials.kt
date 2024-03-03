import org.junit.jupiter.api.Test

class Test_Tutorials {
    @Test
    fun `test basic sqlite`() = main_000()

    @Test
    fun `test basic postgresql`() = main_001()

    @Test
    fun `test query sqlite`() = main_002()

    @Test
    fun `test query postgresql`() = main_003()

    @Test
    fun `test transactions`() = main_004()

    @Test
    fun `test procedures`() = main_005()

    @Test
    fun `test constraints`() = main_006()
    @Test
    fun `test exceptions`() = main_007()
    @Test
    fun `test custom type serializer`() = main_008()
    @Test
    fun `test custom database serializer`() = main_009()
}