package  config
enum Environment {
  case Development, Production, Testing, PreProduction, Staging, QA, Training,
    Config
}

object Environment {
  def setEnv(env: Environment): Unit = {
    env match {
      case Environment.Development => println("Development environment set")
      case Environment.Production  => println("Production environment set")
      case Environment.Testing     => println("Testing environment set")
      case Environment.PreProduction => println("PreProduction environment set")
      case Environment.Staging     => println("Staging environment set")
      case Environment.QA          => println("QA environment set")
      case Environment.Training    => println("Training environment set")
      case Environment.Config      => println("Config environment set")
    }
  }
}