package scala.servlet

class InjectableServletOutputStream(
  write: Int => Unit = i => ()
) extends javax.servlet.ServletOutputStream {
  def write(c: Int) = write.apply(c)
}
