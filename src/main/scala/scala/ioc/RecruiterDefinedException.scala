package scala.ioc

final case class RecruiterDefinedException(namespaceName: String,
    localName:String, private val cause: Throwable = None.orNull)
    extends Exception("A definition for the recruiter in namespace "
        + s"$namespaceName named $localName already exists", cause)
