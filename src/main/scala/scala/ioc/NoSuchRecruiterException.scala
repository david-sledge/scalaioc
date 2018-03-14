package scala.ioc

final case class NoSuchRecruiterException(namespaceName: String,
    localName:String, private val cause: Throwable = None.orNull)
    extends Exception("No recruiter found in namespace "
        + s"$namespaceName named $localName", cause)
