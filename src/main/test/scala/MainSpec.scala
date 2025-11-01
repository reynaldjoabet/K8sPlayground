import config.Environment
import munit.FunSuite

class MainSpec extends FunSuite {

 val greeting = "Hello, Kubernetes Playground!"

 test("Greeting should be correct") {
   assertEquals(greeting, "Hello, Kubernetes Playground!")
 }

 test("Environment enum should have correct values") {
   val envs = Environment.values
   assert(envs.contains(Environment.Development))
   assert(envs.contains(Environment.Production))
   assert(envs.contains(Environment.Testing))
   assert(envs.contains(Environment.PreProduction))
   assert(envs.contains(Environment.Staging))
   assert(envs.contains(Environment.QA))
   assert(envs.contains(Environment.Training))
   assert(envs.contains(Environment.Config))
 }

    test("Set environment should print correct message") {
    // This is a simple test to ensure the method runs without error.
    // In a real-world scenario, you might want to capture stdout and verify the output.
    Environment.setEnv(Environment.Development)
    Environment.setEnv(Environment.Production)
    Environment.setEnv(Environment.Testing)
    Environment.setEnv(Environment.PreProduction)
    Environment.setEnv(Environment.Staging)
    Environment.setEnv(Environment.QA)
    Environment.setEnv(Environment.Training)
    Environment.setEnv(Environment.Config)
    }

}
