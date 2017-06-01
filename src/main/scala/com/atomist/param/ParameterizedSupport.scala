package com.atomist.param

import com.atomist.project.common.{IllformedParametersException, InvalidParametersException, MissingParametersException}

/**
  * Support trait for implementations of Parameterized.
  */
trait ParameterizedSupport
  extends Parameterized {

   override def parameters: Seq[Parameter] = Nil

  /**
    * Fill out any default values not present in pvs but are required.
    *
    * @param pvs parameters so far
    * @return with defaults
    */
  def addDefaultParameterValues(pvs: ParameterValues): ParameterValues = {
    val toDefault = parameters.filter(p => !pvs.parameterValueMap.contains(p.getName) && p.getDefaultValue != "")
    toDefault match {
      case parms: Seq[Parameter] if parms.nonEmpty =>
        val newParams = parms.map(p => SimpleParameterValue(p.getName, p.getDefaultValue))
        new SimpleParameterValues(newParams ++ pvs.parameterValues)
      case _ => pvs
    }
  }

  /**
    * Validate the given arguments, throwing an exception if they're invalid.
    *
    * @param poa arguments to validate
    * @throws InvalidParametersException if the parameters are invalid
    */
  @throws[InvalidParametersException]
  def validateParameters(poa: ParameterValues) {
    val missingParameters = findMissingParameters(poa)
    if (missingParameters.nonEmpty)
      throw new MissingParametersException(
        s"Missing parameters: [${missingParameters.map(_.getName).mkString(",")}]: $poa",
        missingParameters
      )

    def validEmptyOptionalValue(v: Any) = v == null || "".equals(v)

    // TODO could consider pulling this up to Parameterized
    val validationErrors =
      for {
        pv <- poa.parameterValues
        param <- parameters.find(_.name == pv.getName)
        // Only validate optional values if they're supplied
        if param.isRequired || !validEmptyOptionalValue(pv.getValue)
        if !param.isValidValue(pv.getValue)
      } yield pv

    if (validationErrors.nonEmpty)
      throw new IllformedParametersException(s"Invalid parameters: [[${validationErrors.map(p => p.getName + "=" + p.getValue).mkString(",")}]" +
        s": $poa",
        validationErrors)
  }
  /**
    * Convenience method subclasses can use to identify any missing parameters.
    *
    * @param pvs argument passed to an operation on this class
    * @return list of any missing parameters
    */
  def findMissingParameters(pvs: ParameterValues): Seq[Parameter] =
    parameters.filter(p => p.isRequired && !pvs.parameterValueMap.exists(_._1 == p.name))

  def findInvalidParameterValues(pvs: ParameterValues): Seq[ParameterValue] = {
    pvs.parameterValues.foldLeft(Nil: Seq[ParameterValue]) { (acc, pv) =>
      parameters.find(_.getName == pv.getName) match {
        case Some(param) if !param.isValidValue(pv.getValue) => acc :+ pv
        case _ => acc
      }
    }
  }

  /**
    * Convenient method to check whether parameter values are valid.
    * Callers should use findMissingParameters and findInvalidParameterValues to
    * find more information if this method returns false.
    *
    * @param pvs ParameterValues to check
    * @return true if the ParameterValues are valid, otherwise false
    */
  def areValid(pvs: ParameterValues): Boolean =
    findMissingParameters(pvs).isEmpty && findInvalidParameterValues(pvs).isEmpty
}
